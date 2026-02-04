#!/bin/bash
# Milestone 0.2.6 - Robust Prompt Engineering Benchmark
# Uses native llama-simple binary with proper output parsing

ADB="/home/mikhail/Android/Sdk/platform-tools/adb"
DEVICE_DIR="/data/local/tmp/llm-bench"
MODEL="model.gguf"
RESULTS_DIR="/home/mikhail/projects/jeeves/docs/results/0.2.6"
RESULTS_FILE="$RESULTS_DIR/native_benchmark_results.txt"
MAX_TOKENS=15

mkdir -p "$RESULTS_DIR"

echo "=================================================="
echo "Milestone 0.2.6 - Prompt Engineering Benchmark"
echo "=================================================="
echo "Date: $(date)"
echo "Max tokens: $MAX_TOKENS"
echo ""

# Test tasks (5 per quadrant = 20 total)
declare -A TASKS
TASKS[1]="DO|Server is down, customers cannot access"
TASKS[2]="DO|Critical production bug causing data loss"
TASKS[3]="DO|Tax filing deadline is tomorrow"
TASKS[4]="DO|Board presentation starts in 2 hours"
TASKS[5]="DO|VIP customer ticket marked urgent"
TASKS[6]="SCHEDULE|Plan next quarter marketing strategy"
TASKS[7]="SCHEDULE|Read leadership book for career growth"
TASKS[8]="SCHEDULE|Schedule annual health checkup"
TASKS[9]="SCHEDULE|Research new project management tools"
TASKS[10]="SCHEDULE|Write documentation for new feature"
TASKS[11]="DELEGATE|Respond to routine HR survey"
TASKS[12]="DELEGATE|Order office supplies running low"
TASKS[13]="DELEGATE|Schedule team vacation calendar"
TASKS[14]="DELEGATE|Compile weekly team status report"
TASKS[15]="DELEGATE|Book meeting room for team sync"
TASKS[16]="ELIMINATE|Browse social media during break"
TASKS[17]="ELIMINATE|Reorganize email folders again"
TASKS[18]="ELIMINATE|Watch YouTube productivity tips"
TASKS[19]="ELIMINATE|Attend optional picnic planning meeting"
TASKS[20]="ELIMINATE|Clean up old desktop files"

run_inference() {
    local prompt="$1"
    
    # Write prompt to device
    echo "$prompt" | $ADB shell "cat > $DEVICE_DIR/prompt.txt"
    
    # Run inference and capture full output
    local output=$($ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -f prompt.txt -n $MAX_TOKENS" 2>&1)
    
    # Extract the response after <|assistant|>
    local response=$(echo "$output" | grep -A 5 "<|assistant|>" | head -6)
    
    # Parse quadrant from response  
    local result="UNKNOWN"
    if echo "$response" | grep -q "ELIMINATE"; then
        result="ELIMINATE"
    elif echo "$response" | grep -q "DELEGATE"; then
        result="DELEGATE"
    elif echo "$response" | grep -q "SCHEDULE"; then
        result="SCHEDULE"
    elif echo "$response" | grep -q "DO"; then
        result="DO"
    fi
    
    # Extract performance metrics
    local prompt_tps=$(echo "$output" | grep "prompt eval" | grep -oE '[0-9]+\.[0-9]+ tokens per second' | grep -oE '[0-9]+\.[0-9]+')
    local gen_tps=$(echo "$output" | grep "eval time" | grep -oE '[0-9]+\.[0-9]+ tokens per second' | grep -oE '[0-9]+\.[0-9]+')
    
    echo "$result|$prompt_tps|$gen_tps"
}

benchmark_strategy() {
    local strategy_name="$1"
    local prompt_template="$2"
    
    echo ""
    echo "=== Testing: $strategy_name ==="
    echo ""
    
    local correct=0
    local total=0
    local total_prompt_tps=0
    local total_gen_tps=0
    
    for i in $(seq 1 20); do
        local item="${TASKS[$i]}"
        local expected="${item%%|*}"
        local task="${item#*|}"
        
        # Build prompt
        local prompt=$(echo "$prompt_template" | sed "s|{TASK}|$task|g")
        
        # Run inference
        local result=$(run_inference "$prompt")
        local predicted="${result%%|*}"
        local rest="${result#*|}"
        local prompt_tps="${rest%%|*}"
        local gen_tps="${rest#*|}"
        
        total=$((total + 1))
        
        if [ "$predicted" == "$expected" ]; then
            correct=$((correct + 1))
            echo "[✓] #$i $expected → $predicted (${prompt_tps:-?}/${gen_tps:-?} t/s)"
        else
            echo "[✗] #$i $expected → $predicted (${prompt_tps:-?}/${gen_tps:-?} t/s)"
        fi
        
        # Accumulate metrics
        if [ -n "$prompt_tps" ]; then
            total_prompt_tps=$(echo "$total_prompt_tps + $prompt_tps" | bc)
        fi
        if [ -n "$gen_tps" ]; then
            total_gen_tps=$(echo "$total_gen_tps + $gen_tps" | bc)
        fi
    done
    
    local accuracy=$(echo "scale=1; $correct * 100 / $total" | bc)
    local avg_prompt_tps=$(echo "scale=1; $total_prompt_tps / $total" | bc 2>/dev/null || echo "?")
    local avg_gen_tps=$(echo "scale=1; $total_gen_tps / $total" | bc 2>/dev/null || echo "?")
    
    echo ""
    echo "--- $strategy_name Results ---"
    echo "Accuracy: $accuracy% ($correct/$total)"
    echo "Avg Prompt: ${avg_prompt_tps} t/s | Avg Gen: ${avg_gen_tps} t/s"
    
    echo "$strategy_name|$accuracy|$correct|$total|$avg_prompt_tps|$avg_gen_tps" >> "$RESULTS_FILE.csv"
    return $correct
}

# Initialize results
echo "=== Milestone 0.2.6 Prompt Engineering Benchmark ===" > "$RESULTS_FILE"
echo "Date: $(date)" >> "$RESULTS_FILE"
echo "Device: Pixel 9a (Tensor G4, 8GB RAM)" >> "$RESULTS_FILE"
echo "Model: Phi-3-mini-4k-instruct (Q4_K_M, 2.3GB)" >> "$RESULTS_FILE"
echo "Max Tokens: $MAX_TOKENS" >> "$RESULTS_FILE"
echo "Test Cases: 20 (5 per quadrant)" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"

echo "Strategy|Accuracy%|Correct|Total|PromptT/s|GenT/s" > "$RESULTS_FILE.csv"

# Warm up
echo "Warming up model..."
$ADB shell "cd $DEVICE_DIR && export LD_LIBRARY_PATH=. && ./llama-simple -m $MODEL -p 'Hello' -n 3" > /dev/null 2>&1
echo ""

# Strategy 1: BASELINE_SIMPLE
BASELINE='<|user|>
Classify using Eisenhower Matrix:
- DO: Urgent and Important  
- SCHEDULE: Important but Not Urgent
- DELEGATE: Urgent but Not Important
- ELIMINATE: Neither urgent nor important

Task: "{TASK}"

Answer with one word: DO, SCHEDULE, DELEGATE, or ELIMINATE
<|end|>
<|assistant|>'

benchmark_strategy "BASELINE_SIMPLE" "$BASELINE"
baseline_correct=$?

# Strategy 2: JSON_STRUCTURED (0.2.6.1)
JSON='<|user|>
Classify this task. Respond with JSON only:
{"quadrant": "DO|SCHEDULE|DELEGATE|ELIMINATE"}

Rules:
- DO = deadline within 48h AND affects goals/customers
- SCHEDULE = important for long-term, no deadline  
- DELEGATE = has deadline but can be done by others
- ELIMINATE = not important, time waster

Task: "{TASK}"
<|end|>
<|assistant|>'

benchmark_strategy "JSON_STRUCTURED" "$JSON"
json_correct=$?

# Strategy 3: CHAIN_OF_THOUGHT (0.2.6.2)
COT='<|user|>
Classify this task step by step:

Task: "{TASK}"

Step 1: Is there a deadline within 48h or crisis? (URGENT/NOT_URGENT)
Step 2: Does it affect goals, customers, or health? (IMPORTANT/NOT_IMPORTANT)
Step 3: Based on steps above, classify as:
- URGENT + IMPORTANT = DO
- NOT_URGENT + IMPORTANT = SCHEDULE
- URGENT + NOT_IMPORTANT = DELEGATE
- NOT_URGENT + NOT_IMPORTANT = ELIMINATE

Final answer (one word):
<|end|>
<|assistant|>'

benchmark_strategy "CHAIN_OF_THOUGHT" "$COT"
cot_correct=$?

# Strategy 4: FEW_SHOT (0.2.6.3)  
FEWSHOT='<|user|>
Examples:
"Server down" → DO
"Plan strategy" → SCHEDULE
"Order supplies" → DELEGATE
"Social media" → ELIMINATE

Classify: "{TASK}"
Answer:
<|end|>
<|assistant|>'

benchmark_strategy "FEW_SHOT" "$FEWSHOT"
fewshot_correct=$?

# Strategy 5: EXPERT_PERSONA (0.2.6.4)
EXPERT='<|system|>
You are a productivity expert using the Eisenhower Matrix.
URGENT = deadline ≤48h OR crisis OR someone waiting
IMPORTANT = affects goals/customers/health/relationships
DO = urgent AND important
SCHEDULE = important but NOT urgent  
DELEGATE = urgent but NOT important
ELIMINATE = neither urgent nor important
<|end|>
<|user|>
Classify: "{TASK}"
Answer one word:
<|end|>
<|assistant|>'

benchmark_strategy "EXPERT_PERSONA" "$EXPERT"
expert_correct=$?

# Strategy 6: COMBINED_OPTIMAL
COMBINED='<|system|>
Eisenhower Matrix expert. Answer with one word only.
<|end|>
<|user|>
Examples:
Server down → DO (crisis)
Plan strategy → SCHEDULE (important)
Order supplies → DELEGATE (routine)
Social media → ELIMINATE (time waster)

Classify: "{TASK}"
<|end|>
<|assistant|>'

benchmark_strategy "COMBINED_OPTIMAL" "$COMBINED"
combined_correct=$?

# Final summary
echo ""
echo "=================================================="
echo "BENCHMARK COMPLETE"
echo "=================================================="
echo ""
echo "Results Summary:"
cat "$RESULTS_FILE.csv" | column -t -s '|'
echo ""

# Find best strategy
best_name=""
best_acc=0
while IFS='|' read -r name acc correct total ptps gtps; do
    if [ "$name" != "Strategy" ] && [ -n "$acc" ]; then
        acc_int=$(echo "$acc" | cut -d. -f1)
        if [ "$acc_int" -gt "$best_acc" ]; then
            best_acc=$acc_int
            best_name=$name
        fi
    fi
done < "$RESULTS_FILE.csv"

echo "Best Strategy: $best_name ($best_acc%)"
echo ""

# Append summary to results file
echo "" >> "$RESULTS_FILE"
echo "=== Results ===" >> "$RESULTS_FILE"
cat "$RESULTS_FILE.csv" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"
echo "Best Strategy: $best_name ($best_acc%)" >> "$RESULTS_FILE"

echo "Full results saved to: $RESULTS_FILE"
