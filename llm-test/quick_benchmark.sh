#!/bin/bash
# Milestone 0.2.6 - Quick Prompt Engineering Benchmark
# Tests 5 representative tasks per strategy for rapid results
# 
# Expected runtime: ~10-15 minutes

set -e

ADB=${ADB:-/home/mikhail/Android/Sdk/platform-tools/adb}
DEVICE_DIR="/data/local/tmp/llm-bench"
MODEL="model.gguf"
RESULTS_FILE="/home/mikhail/projects/jeeves/llm-test/quick_benchmark_results.txt"
MAX_TOKENS=30

echo "=================================================="
echo "Milestone 0.2.6 - Quick Prompt Benchmark (5 tasks)"
echo "=================================================="
echo "Date: $(date)"
echo ""

# Verify device
$ADB devices | grep -q "device$" || { echo "No device connected!"; exit 1; }

# Representative tasks (5 tasks, 1-2 per quadrant)
declare -a QUICK_TASKS=(
    "DO|Server is down, customers cannot access the app"
    "DO|Tax filing deadline is tomorrow"
    "SCHEDULE|Plan next quarter marketing strategy"
    "DELEGATE|Order office supplies running low"
    "ELIMINATE|Browse social media during break"
)

run_single_test() {
    local strategy=$1
    local expected=$2
    local task=$3
    local idx=$4
    
    # Write prompt file to device
    $ADB shell "cat > $DEVICE_DIR/prompt.txt" << EOF
$prompt_content
EOF
    
    # Run inference and capture timing
    local start_time=$(date +%s%N)
    local output=$($ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && timeout 120 ./llama-simple -m $MODEL -f prompt.txt -n $MAX_TOKENS" 2>&1 || echo "TIMEOUT")
    local end_time=$(date +%s%N)
    local elapsed_ms=$(( (end_time - start_time) / 1000000 ))
    
    # Parse result
    local result="UNKNOWN"
    if echo "$output" | grep -qi '"quadrant"[^}]*"ELIMINATE"'; then
        result="ELIMINATE"
    elif echo "$output" | grep -qi '"quadrant"[^}]*"DELEGATE"'; then
        result="DELEGATE"
    elif echo "$output" | grep -qi '"quadrant"[^}]*"SCHEDULE"'; then
        result="SCHEDULE"
    elif echo "$output" | grep -qi '"quadrant"[^}]*"DO"'; then
        result="DO"
    # Fallback keyword detection
    elif echo "$output" | grep -q "ELIMINATE"; then
        result="ELIMINATE"
    elif echo "$output" | grep -q "DELEGATE"; then
        result="DELEGATE"
    elif echo "$output" | grep -q "SCHEDULE"; then
        result="SCHEDULE"
    elif echo "$output" | grep -q "[^A-Z]DO[^A-Z]"; then
        result="DO"
    fi
    
    local tps=$(echo "$output" | grep -oP 'prompt eval.*?(\d+\.?\d*)\s*tokens per second' | grep -oP '\d+\.?\d*' | tail -1)
    local gen_tps=$(echo "$output" | grep -oP 'eval time.*?(\d+\.?\d*)\s*tokens per second' | grep -oP '\d+\.?\d*' | tail -1)
    
    if [ "$result" == "$expected" ]; then
        echo "[$idx] ✓ $expected → $result (${elapsed_ms}ms, prompt:${tps:-?}t/s gen:${gen_tps:-?}t/s)"
        return 1
    else
        echo "[$idx] ✗ $expected → $result (${elapsed_ms}ms)"
        return 0
    fi
}

test_strategy() {
    local strategy_name=$1
    shift
    local prompt_template="$@"
    
    echo ""
    echo "--- Testing: $strategy_name ---"
    
    local correct=0
    local total=0
    
    for item in "${QUICK_TASKS[@]}"; do
        total=$((total + 1))
        local expected="${item%%|*}"
        local task="${item#*|}"
        
        # Build prompt using template
        prompt_content=$(echo "$prompt_template" | sed "s|{TASK}|$task|g")
        
        run_single_test "$strategy_name" "$expected" "$task" "$total"
        if [ $? -eq 1 ]; then
            correct=$((correct + 1))
        fi
    done
    
    local accuracy=$(echo "scale=1; $correct * 100 / $total" | bc)
    echo "Result: $accuracy% ($correct/$total)"
    echo "$strategy_name: $accuracy% ($correct/$total)" >> "$RESULTS_FILE"
}

# Initialize results file
echo "=== Quick Prompt Benchmark Results ===" > "$RESULTS_FILE"
echo "Date: $(date)" >> "$RESULTS_FILE"
echo "Tasks: 5 per strategy" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"

# Warm-up
echo "Warming up model..."
$ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -p 'Hello world' -n 5" > /dev/null 2>&1
echo ""

# Strategy 1: BASELINE_SIMPLE
BASELINE='<|user|>
Classify this task into an Eisenhower Matrix quadrant.
- DO: Urgent and Important
- SCHEDULE: Important but Not Urgent
- DELEGATE: Urgent but Not Important  
- ELIMINATE: Not Urgent and Not Important

Task: "{TASK}"

Respond with JSON: {"quadrant": "...", "confidence": 0.X, "reasoning": "..."}
<|end|>
<|assistant|>'

test_strategy "BASELINE_SIMPLE" "$BASELINE"

# Strategy 2: JSON_STRUCTURED (0.2.6.1)
JSON_STRUCTURED='<|user|>
You must respond with ONLY valid JSON:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "..."}

Rules:
- DO = deadline ≤48h AND important
- SCHEDULE = important, no deadline
- DELEGATE = deadline but not important
- ELIMINATE = neither urgent nor important

Task: "{TASK}"
<|end|>
<|assistant|>'

test_strategy "JSON_STRUCTURED" "$JSON_STRUCTURED"

# Strategy 3: EXPERT_PERSONA (0.2.6.4)
EXPERT_PERSONA='<|system|>
You are Dr. Priority, an Eisenhower Matrix expert.
- URGENT = deadline ≤48h OR crisis OR someone waiting
- IMPORTANT = affects goals/customers/health
- DO = urgent AND important
- SCHEDULE = NOT urgent AND important
- DELEGATE = urgent AND NOT important
- ELIMINATE = NOT urgent AND NOT important
Respond with JSON only.
<|end|>
<|user|>
Task: "{TASK}"
{"quadrant":"?","confidence":?,"reasoning":"?"}
<|end|>
<|assistant|>'

test_strategy "EXPERT_PERSONA" "$EXPERT_PERSONA"

# Strategy 4: FEW_SHOT (0.2.6.3)
FEW_SHOT='<|user|>
Examples:
"Server down" → {"quadrant":"DO","confidence":0.95,"reasoning":"crisis"}
"Plan strategy" → {"quadrant":"SCHEDULE","confidence":0.85,"reasoning":"important planning"}
"Order supplies" → {"quadrant":"DELEGATE","confidence":0.75,"reasoning":"routine admin"}
"Social media" → {"quadrant":"ELIMINATE","confidence":0.90,"reasoning":"time waster"}

Classify: "{TASK}"
<|end|>
<|assistant|>'

test_strategy "FEW_SHOT" "$FEW_SHOT"

# Strategy 5: COMBINED_OPTIMAL
COMBINED='<|system|>
Eisenhower Matrix classifier. JSON only.
DO=urgent+important, SCHEDULE=important, DELEGATE=urgent, ELIMINATE=neither
<|end|>
<|user|>
Ex: "Server down"→DO, "Plan strategy"→SCHEDULE, "Order supplies"→DELEGATE, "Social media"→ELIMINATE

Classify: "{TASK}"
{"quadrant":"?","confidence":?,"reasoning":"?"}
<|end|>
<|assistant|>'

test_strategy "COMBINED_OPTIMAL" "$COMBINED"

echo ""
echo "=================================================="
echo "BENCHMARK COMPLETE"
echo "=================================================="
cat "$RESULTS_FILE"
echo ""
echo "Full results saved to: $RESULTS_FILE"
