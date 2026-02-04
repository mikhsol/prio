#!/bin/bash
# Milestone 0.2.6 - Full 20-Task Prompt Engineering Benchmark
# Fixed: inline prompts, DELETE→ELIMINATE synonym handling

ADB="/home/mikhail/Android/Sdk/platform-tools/adb"
DEVICE_DIR="/data/local/tmp/llm-bench"
MODEL="model.gguf"
RESULTS_DIR="/home/mikhail/projects/jeeves/docs/results/0.2.6"
MAX_TOKENS=15

mkdir -p "$RESULTS_DIR"

echo "=================================================="
echo "Milestone 0.2.6 - Full 20-Task Benchmark"
echo "=================================================="
echo "Date: $(date)"
echo ""

# 20 test cases (5 per quadrant)
declare -a TASKS=(
    "DO|Server is down and customers cannot access"
    "DO|Critical production bug causing data loss"
    "DO|Tax filing deadline is tomorrow"
    "DO|Board presentation starts in 2 hours"
    "DO|VIP customer support ticket marked urgent"
    "SCHEDULE|Plan next quarter marketing strategy"
    "SCHEDULE|Read leadership book for career growth"
    "SCHEDULE|Schedule annual health checkup"
    "SCHEDULE|Research new project management tools"
    "SCHEDULE|Write documentation for new feature"
    "DELEGATE|Respond to routine HR survey"
    "DELEGATE|Order office supplies running low"
    "DELEGATE|Schedule team vacation calendar"
    "DELEGATE|Compile weekly team status report"
    "DELEGATE|Book meeting room for team sync"
    "ELIMINATE|Browse social media during break"
    "ELIMINATE|Reorganize email folders again"
    "ELIMINATE|Watch YouTube productivity tips"
    "ELIMINATE|Attend optional picnic planning meeting"
    "ELIMINATE|Clean up old desktop files"
)

run_test() {
    local prompt="$1"
    local expected="$2"
    local idx="$3"
    local task="$4"
    
    # Run inference
    local output=$($ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -n $MAX_TOKENS '$prompt'" 2>&1)
    
    # Get response line
    local response=$(echo "$output" | grep "^<s>" | head -1)
    local after_assistant=$(echo "$response" | sed 's/.*<|assistant|>//')
    
    # Parse with DELETE→ELIMINATE handling
    local result="UNKNOWN"
    if echo "$after_assistant" | grep -qiE "(ELIMINATE|DELETE)"; then
        result="ELIMINATE"
    elif echo "$after_assistant" | grep -qi "DELEGATE"; then
        result="DELEGATE"
    elif echo "$after_assistant" | grep -qi "SCHEDULE"; then
        result="SCHEDULE"
    elif echo "$after_assistant" | grep -qiE "( DO|^DO)"; then
        result="DO"
    fi
    
    local prompt_tps=$(echo "$output" | grep "prompt eval" | grep -oE '[0-9]+\.[0-9]+\s*tokens per second' | head -1 | grep -oE '^[0-9]+\.[0-9]+')
    
    if [ "$result" == "$expected" ]; then
        echo "[✓] #$idx $expected → $result (${prompt_tps:-?} t/s) | ${task:0:35}"
        return 1
    else
        echo "[✗] #$idx $expected → $result (${prompt_tps:-?} t/s) | ${task:0:35}"
        return 0
    fi
}

test_strategy() {
    local name="$1"
    local template="$2"
    
    echo ""
    echo "=== Testing: $name ==="
    
    local correct=0
    local total=0
    local do_correct=0
    local schedule_correct=0
    local delegate_correct=0
    local eliminate_correct=0
    
    for i in "${!TASKS[@]}"; do
        local item="${TASKS[$i]}"
        local expected="${item%%|*}"
        local task="${item#*|}"
        
        # Build single-line prompt
        local prompt=$(echo "$template" | tr '\n' ' ' | sed "s|{TASK}|$task|g")
        
        total=$((total + 1))
        run_test "$prompt" "$expected" "$total" "$task"
        if [ $? -eq 1 ]; then
            correct=$((correct + 1))
            case $expected in
                DO) do_correct=$((do_correct + 1)) ;;
                SCHEDULE) schedule_correct=$((schedule_correct + 1)) ;;
                DELEGATE) delegate_correct=$((delegate_correct + 1)) ;;
                ELIMINATE) eliminate_correct=$((eliminate_correct + 1)) ;;
            esac
        fi
    done
    
    local accuracy=$(echo "scale=1; $correct * 100 / $total" | bc)
    echo ""
    echo "--- $name Results ---"
    echo "Overall: $accuracy% ($correct/$total)"
    echo "DO: $do_correct/5 | SCHEDULE: $schedule_correct/5 | DELEGATE: $delegate_correct/5 | ELIMINATE: $eliminate_correct/5"
    
    echo "$name|$accuracy|$correct|$do_correct|$schedule_correct|$delegate_correct|$eliminate_correct" >> "$RESULTS_DIR/full_results.csv"
}

# Initialize
echo "Strategy|Accuracy|Correct|DO|SCHEDULE|DELEGATE|ELIMINATE" > "$RESULTS_DIR/full_results.csv"

# Warm up
echo "Warming up..."
$ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -n 3 'Hello'" > /dev/null 2>&1

# Strategy 1: BASELINE (simple)
test_strategy "BASELINE" '<|user|>Task: {TASK}. Classify as DO, SCHEDULE, DELEGATE, or ELIMINATE. Answer one word.<|end|><|assistant|>'

# Strategy 2: EXPERT_PERSONA (0.2.6.4)
test_strategy "EXPERT_PERSONA" '<|system|>You are an Eisenhower Matrix expert. Classify tasks as DO (urgent+important), SCHEDULE (important), DELEGATE (urgent only), or ELIMINATE (neither). Answer one word only.<|end|><|user|>{TASK}<|end|><|assistant|>'

# Strategy 3: FEW_SHOT with correct format (0.2.6.3)
test_strategy "FEW_SHOT" '<|user|>"Server down customers affected"→DO. "Plan quarterly strategy"→SCHEDULE. "Order office supplies"→DELEGATE. "Watch YouTube"→ELIMINATE. Now classify: "{TASK}"<|end|><|assistant|>'

# Strategy 4: COMBINED with short examples
test_strategy "COMBINED" '<|system|>Eisenhower classifier. DO=crisis/deadline. SCHEDULE=important. DELEGATE=routine. ELIMINATE=timewaster.<|end|><|user|>Classify: {TASK}<|end|><|assistant|>'

echo ""
echo "=================================================="
echo "BENCHMARK COMPLETE"
echo "=================================================="
echo ""
echo "Results Summary:"
column -t -s '|' "$RESULTS_DIR/full_results.csv"
echo ""
echo "Results saved to: $RESULTS_DIR/full_results.csv"

# Save detailed results
{
    echo "=== Milestone 0.2.6 Prompt Engineering Benchmark ==="
    echo "Date: $(date)"
    echo "Device: Pixel 9a (Tensor G4, 8GB RAM)"
    echo "Model: Phi-3-mini-4k-instruct (Q4_K_M)"
    echo "Test Cases: 20 (5 per quadrant)"
    echo ""
    column -t -s '|' "$RESULTS_DIR/full_results.csv"
} > "$RESULTS_DIR/benchmark_summary.txt"
