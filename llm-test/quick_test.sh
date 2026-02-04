#!/bin/bash
# Milestone 0.2.6 - Corrected Prompt Engineering Benchmark
# Uses inline prompts (llama-simple doesn't support -f file input)

ADB="/home/mikhail/Android/Sdk/platform-tools/adb"
DEVICE_DIR="/data/local/tmp/llm-bench"
MODEL="model.gguf"
RESULTS_DIR="/home/mikhail/projects/jeeves/docs/results/0.2.6"
MAX_TOKENS=20

mkdir -p "$RESULTS_DIR"

echo "=================================================="
echo "Milestone 0.2.6 - Prompt Engineering Benchmark"
echo "=================================================="
echo "Date: $(date)"
echo ""

# Quick 5-task test (1-2 per quadrant for fast iteration)
declare -a QUICK_TASKS=(
    "DO|Server is down and customers cannot access"
    "DO|Tax deadline is tomorrow"
    "SCHEDULE|Plan next quarter strategy"  
    "DELEGATE|Order office supplies"
    "ELIMINATE|Browse social media"
)

run_test() {
    local prompt="$1"
    local expected="$2"
    local idx="$3"
    
    # Escape prompt for shell
    local escaped_prompt=$(echo "$prompt" | sed "s/'/'\\\\''/g")
    
    # Run inference with inline prompt
    local output=$($ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -n $MAX_TOKENS '$escaped_prompt'" 2>&1)
    
    # Extract the response line (starts with <s>)
    local response=$(echo "$output" | grep "^<s>" | head -1)
    
    # Parse quadrant from response (check after <|assistant|>)
    local after_assistant=$(echo "$response" | sed 's/.*<|assistant|>//')
    
    local result="UNKNOWN"
    if echo "$after_assistant" | grep -qi "ELIMINATE"; then
        result="ELIMINATE"
    elif echo "$after_assistant" | grep -qi "DELEGATE"; then
        result="DELEGATE"
    elif echo "$after_assistant" | grep -qi "SCHEDULE"; then
        result="SCHEDULE"
    elif echo "$after_assistant" | grep -qi "DO"; then
        result="DO"
    fi
    
    # Extract metrics
    local prompt_tps=$(echo "$output" | grep "prompt eval" | grep -oE '[0-9]+\.[0-9]+\s*tokens per second' | grep -oE '^[0-9]+\.[0-9]+')
    
    if [ "$result" == "$expected" ]; then
        echo "[✓] #$idx $expected → $result (${prompt_tps:-?} t/s)"
        return 1
    else
        echo "[✗] #$idx $expected → $result (${prompt_tps:-?} t/s) | Response: ${after_assistant:0:50}"
        return 0
    fi
}

test_strategy() {
    local name="$1"
    shift
    local template="$1"
    
    echo ""
    echo "=== $name ==="
    
    local correct=0
    local total=0
    
    for i in "${!QUICK_TASKS[@]}"; do
        local item="${QUICK_TASKS[$i]}"
        local expected="${item%%|*}"
        local task="${item#*|}"
        
        # Build prompt from template
        local prompt=$(echo "$template" | sed "s|{TASK}|$task|g")
        
        total=$((total + 1))
        run_test "$prompt" "$expected" "$total"
        if [ $? -eq 1 ]; then
            correct=$((correct + 1))
        fi
    done
    
    local accuracy=$(echo "scale=0; $correct * 100 / $total" | bc)
    echo "Result: $accuracy% ($correct/$total)"
    echo "$name: $accuracy% ($correct/$total)" >> "$RESULTS_DIR/quick_results.txt"
}

# Initialize
echo "=== Quick Benchmark $(date) ===" > "$RESULTS_DIR/quick_results.txt"

# Warm up
echo "Warming up..."
$ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -n 3 'Hello'" > /dev/null 2>&1
echo ""

# Strategy 1: Simple baseline
test_strategy "BASELINE" '<|user|>Task: {TASK}. Classify as DO, SCHEDULE, DELEGATE, or ELIMINATE. One word only.<|end|><|assistant|>'

# Strategy 2: With definitions
test_strategy "WITH_DEFS" '<|user|>Eisenhower Matrix:
DO=Urgent+Important, SCHEDULE=Important, DELEGATE=Urgent only, ELIMINATE=Neither.
Task: {TASK}
Answer one word:<|end|><|assistant|>'

# Strategy 3: Few-shot
test_strategy "FEW_SHOT" '<|user|>Examples: "Server down"=DO, "Plan strategy"=SCHEDULE, "Order supplies"=DELEGATE, "Social media"=ELIMINATE.
Classify: {TASK}
Answer:<|end|><|assistant|>'

# Strategy 4: Expert with examples
test_strategy "EXPERT" '<|system|>You classify tasks using Eisenhower Matrix. Answer one word only: DO, SCHEDULE, DELEGATE, or ELIMINATE.<|end|><|user|>{TASK}<|end|><|assistant|>'

echo ""
echo "=== COMPLETE ==="
cat "$RESULTS_DIR/quick_results.txt"
