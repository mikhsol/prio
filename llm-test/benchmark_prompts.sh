#!/bin/bash
# Milestone 0.2.6 - Prompt Engineering Benchmark Script
# Uses native llama-simple binary for optimal performance (20+ t/s)
# 
# Run this on the host machine with: ./benchmark_prompts.sh

set -e

ADB=${ADB:-/home/mikhail/Android/Sdk/platform-tools/adb}
DEVICE_DIR="/data/local/tmp/llm-bench"
MODEL="model.gguf"
RESULTS_FILE="benchmark_0.2.6_results.txt"
MAX_TOKENS=30

echo "=================================================="
echo "Milestone 0.2.6 - Prompt Engineering Benchmark"
echo "=================================================="
echo "Date: $(date)"
echo "Device: $($ADB shell getprop ro.product.model)"
echo "Model: Phi-3-mini-4k-instruct (Q4_K_M)"
echo ""

# Verify device connection
$ADB devices | grep -q "device$" || { echo "No device connected!"; exit 1; }

# Verify model and binary exist
$ADB shell "test -f $DEVICE_DIR/$MODEL" || { echo "Model not found!"; exit 1; }
$ADB shell "test -f $DEVICE_DIR/llama-simple" || { echo "llama-simple not found!"; exit 1; }

# Test cases (20 diverse tasks)
declare -a TASKS=(
    "DO|Server is down, customers cannot access the app"
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

# Function to run a single test
run_test() {
    local strategy=$1
    local prompt_file=$2
    local task=$3
    local expected=$4
    local idx=$5
    
    # Copy prompt to device
    echo "$prompt_file" | $ADB shell "cat > $DEVICE_DIR/test_prompt.txt"
    
    # Run inference
    start_time=$(date +%s%N)
    output=$($ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -f test_prompt.txt -n $MAX_TOKENS" 2>&1)
    end_time=$(date +%s%N)
    elapsed_ms=$(( (end_time - start_time) / 1000000 ))
    
    # Parse result
    if echo "$output" | grep -q '"quadrant".*:.*"ELIMINATE"'; then
        result="ELIMINATE"
    elif echo "$output" | grep -q '"quadrant".*:.*"DELEGATE"'; then
        result="DELEGATE"
    elif echo "$output" | grep -q '"quadrant".*:.*"SCHEDULE"'; then
        result="SCHEDULE"
    elif echo "$output" | grep -q '"quadrant".*:.*"DO"'; then
        result="DO"
    # Fallback to raw keyword detection
    elif echo "$output" | grep -q "ELIMINATE"; then
        result="ELIMINATE"
    elif echo "$output" | grep -q "DELEGATE"; then
        result="DELEGATE"
    elif echo "$output" | grep -q "SCHEDULE"; then
        result="SCHEDULE"
    elif echo "$output" | grep -q " DO"; then
        result="DO"
    else
        result="UNKNOWN"
    fi
    
    # Extract tokens/s from output
    tps=$(echo "$output" | grep "prompt eval" | grep -oP '[\d.]+(?= tokens per second)' | head -1)
    
    if [ "$result" == "$expected" ]; then
        status="✓"
    else
        status="✗"
    fi
    
    echo "[$status] #$idx | Expected=$expected Got=$result | ${elapsed_ms}ms | ${tps:-?}t/s"
    
    # Return 1 if correct, 0 if wrong
    [ "$result" == "$expected" ] && return 1 || return 0
}

# Strategy prompts
build_baseline_prompt() {
    local task="$1"
    cat << EOF
<|user|>
Classify this task into an Eisenhower Matrix quadrant.

Quadrants:
- DO: Urgent and Important
- SCHEDULE: Important but Not Urgent
- DELEGATE: Urgent but Not Important
- ELIMINATE: Not Urgent and Not Important

Task: "$task"

Respond with JSON only:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE", "confidence": 0.0-1.0, "reasoning": "brief explanation"}
<|end|>
<|assistant|>
EOF
}

build_json_structured_prompt() {
    local task="$1"
    cat << EOF
<|user|>
You must respond with ONLY valid JSON matching this exact schema:
{
  "quadrant": "<string: exactly one of DO, SCHEDULE, DELEGATE, ELIMINATE>",
  "confidence": <number: between 0.0 and 1.0>,
  "reasoning": "<string: one sentence explanation>"
}

Classification rules:
- DO = deadline within 24-48 hours AND impacts goals/customers/revenue
- SCHEDULE = important for long-term success, no immediate deadline
- DELEGATE = has deadline but low personal value, could be done by others
- ELIMINATE = no deadline AND no real value, time waster

Task to classify: "$task"

JSON response:
<|end|>
<|assistant|>
EOF
}

build_expert_persona_prompt() {
    local task="$1"
    cat << EOF
<|system|>
You are Dr. Priority, a world-renowned time management expert who has helped thousands of executives master the Eisenhower Matrix. You have three decision rules:

RULE 1 - URGENCY: A task is URGENT only if:
- There's a hard deadline within 48 hours, OR
- Someone is actively waiting/blocked, OR
- There's an active crisis/emergency

RULE 2 - IMPORTANCE: A task is IMPORTANT only if:
- It directly contributes to career/business/health goals, OR
- It affects customers, revenue, or relationships, OR
- Failure would have significant consequences

RULE 3 - QUADRANT MAPPING:
- DO: URGENT AND IMPORTANT (crises, deadlines with consequences)
- SCHEDULE: NOT URGENT AND IMPORTANT (planning, growth, prevention)
- DELEGATE: URGENT AND NOT IMPORTANT (interruptions, admin tasks)
- ELIMINATE: NOT URGENT AND NOT IMPORTANT (time wasters, distractions)

You always respond with valid JSON only.
<|end|>
<|user|>
Classify this task: "$task"

{"quadrant": "?", "confidence": ?, "reasoning": "?"}
<|end|>
<|assistant|>
EOF
}

build_few_shot_prompt() {
    local task="$1"
    cat << EOF
<|user|>
Classify tasks into Eisenhower Matrix quadrants. Here are examples:

DO (Urgent + Important):
- "Server is down, customers cannot access" → {"quadrant":"DO","confidence":0.95,"reasoning":"crisis affecting customers"}
- "Tax filing deadline is tomorrow" → {"quadrant":"DO","confidence":0.90,"reasoning":"legal deadline imminent"}

SCHEDULE (Not Urgent + Important):
- "Plan next quarter strategy" → {"quadrant":"SCHEDULE","confidence":0.85,"reasoning":"important planning, no deadline"}
- "Schedule annual health checkup" → {"quadrant":"SCHEDULE","confidence":0.80,"reasoning":"health matters, not urgent"}

DELEGATE (Urgent + Not Important):
- "Order office supplies by EOD" → {"quadrant":"DELEGATE","confidence":0.75,"reasoning":"deadline exists, not strategic"}
- "Respond to routine HR survey today" → {"quadrant":"DELEGATE","confidence":0.70,"reasoning":"deadline, low value"}

ELIMINATE (Not Urgent + Not Important):
- "Browse social media" → {"quadrant":"ELIMINATE","confidence":0.90,"reasoning":"time waster"}
- "Reorganize desktop icons" → {"quadrant":"ELIMINATE","confidence":0.85,"reasoning":"busy work"}

Now classify this task:
"$task"

Respond with JSON only:
<|end|>
<|assistant|>
EOF
}

build_combined_optimal_prompt() {
    local task="$1"
    cat << EOF
<|system|>
You are an Eisenhower Matrix classification expert. You analyze tasks methodically and respond with JSON only.

Decision Framework:
- URGENT = deadline ≤48h OR crisis OR someone waiting
- IMPORTANT = affects goals/customers/health/relationships

Quadrant Rules:
- DO: urgent AND important (handle now)
- SCHEDULE: important but NOT urgent (plan for later)
- DELEGATE: urgent but NOT important (assign to others)
- ELIMINATE: neither urgent nor important (remove)
<|end|>
<|user|>
Examples:
1. "Server down affecting customers" → {"quadrant":"DO","confidence":0.95,"reasoning":"Active crisis affecting customers"}
2. "Plan Q2 marketing strategy" → {"quadrant":"SCHEDULE","confidence":0.85,"reasoning":"Strategic planning without deadline"}
3. "Order supplies by EOD" → {"quadrant":"DELEGATE","confidence":0.75,"reasoning":"Deadline exists but routine admin task"}
4. "Browse social media" → {"quadrant":"ELIMINATE","confidence":0.90,"reasoning":"Time waster with no value"}

Now classify: "$task"
JSON only:
<|end|>
<|assistant|>
EOF
}

# Run benchmark for a strategy
run_strategy_benchmark() {
    local strategy_name=$1
    local prompt_func=$2
    
    echo ""
    echo "=================================================="
    echo "Strategy: $strategy_name"
    echo "=================================================="
    
    correct=0
    total=0
    total_time=0
    
    for item in "${TASKS[@]}"; do
        expected="${item%%|*}"
        task="${item#*|}"
        
        total=$((total + 1))
        
        prompt=$($prompt_func "$task")
        
        # Run test
        run_test "$strategy_name" "$prompt" "$task" "$expected" "$total"
        result=$?
        
        if [ $result -eq 1 ]; then
            correct=$((correct + 1))
        fi
    done
    
    accuracy=$(echo "scale=1; $correct * 100 / $total" | bc)
    echo ""
    echo "--- $strategy_name Summary ---"
    echo "Accuracy: $accuracy% ($correct/$total)"
    echo ""
    
    # Save results
    echo "$strategy_name: $accuracy% ($correct/$total)" >> $RESULTS_FILE
}

# Main benchmark execution
echo "" > $RESULTS_FILE
echo "=== Milestone 0.2.6 Prompt Engineering Benchmark ===" >> $RESULTS_FILE
echo "Date: $(date)" >> $RESULTS_FILE
echo "Device: $($ADB shell getprop ro.product.model)" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

echo ""
echo "Running warm-up inference..."
$ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -p 'Hello' -n 5" > /dev/null 2>&1

# Run each strategy
echo ""
echo "Starting benchmark with 20 tasks per strategy..."

run_strategy_benchmark "BASELINE_SIMPLE" build_baseline_prompt
run_strategy_benchmark "JSON_STRUCTURED" build_json_structured_prompt  
run_strategy_benchmark "EXPERT_PERSONA" build_expert_persona_prompt
run_strategy_benchmark "FEW_SHOT" build_few_shot_prompt
run_strategy_benchmark "COMBINED_OPTIMAL" build_combined_optimal_prompt

echo ""
echo "=================================================="
echo "BENCHMARK COMPLETE"
echo "=================================================="
cat $RESULTS_FILE
echo ""
echo "Results saved to: $RESULTS_FILE"
