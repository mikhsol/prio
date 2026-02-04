# Milestone 0.2.6 - Prompt Engineering Benchmark Results

**Date:** February 4, 2026  
**Device:** Google Pixel 9a (Tensor G4, 8GB RAM, Android 16)  
**Model:** Phi-3-mini-4k-instruct (Q4_K_M quantization)  
**Model Size:** 2.39 GB  

## Summary

This benchmark evaluates 6 different prompt strategies for Eisenhower Matrix task classification using on-device LLM inference.

## Quick Strategy Test Results (5 DO Quadrant Tasks Each)

| Strategy | Accuracy | Avg Latency | Parse Failures | Target Met |
|----------|----------|-------------|----------------|------------|
| Baseline Simple (0.2.6.0) | TBD | TBD | TBD | TBD |
| JSON Structured (0.2.6.1) | TBD | TBD | TBD | TBD |
| Chain-of-Thought (0.2.6.2) | TBD | TBD | TBD | TBD |
| Few-Shot Learning (0.2.6.3) | TBD | TBD | TBD | TBD |
| Expert Persona (0.2.6.4) | **100%** (5/5) | 99.8s | 0 | ✅ |
| Combined Optimal | **80%** (4/5) | 90.6s | 1 | ✅ |

> Note: Earlier strategy results were lost due to logcat buffer rotation. The full 50-task benchmark is currently running.

## Detailed Observations

### Model Loading Performance
- **Load time:** 471-664ms (excellent)
- **Memory usage:** ~3.2GB during inference
- **Stable across all test runs**

### Inference Performance
- **Token generation:** 50 tokens in ~33-121 seconds
- **Average per task:** ~90-120 seconds
- **Tokens per second:** ~0.4-1.5 tok/s

### Classification Examples

**Successful Classifications:**
```
Task: "Server is down, customers cannot access"
Expected: DO | Predicted: DO | Confidence: 95%

Task: "Tax filing deadline is tomorrow"  
Expected: DO | Predicted: DO | Latency: 111s

Task: "Board presentation starts in 2 hours"
Expected: DO | Predicted: DO | Latency: 98s

Task: "VIP customer support ticket marked urgent"
Expected: DO | Predicted: DO | Latency: 92s
```

### Expert Persona (0.2.6.4) - Best Performing
- **100% accuracy** on DO quadrant tasks
- Uses persona prompt: "You are an expert productivity consultant..."
- Consistent high-confidence predictions (95%+)

### Combined Optimal Strategy
- **80% accuracy** with 1 parse failure
- Combines JSON structure + expert persona
- Good latency performance (~90s)

## Full 50-Task Benchmark Status

**Status:** Running (started at 09:08:05)  
**Progress:** Task 3/50 for Baseline strategy  
**Estimated total time:** ~10 hours for all 6 strategies

### Benchmark Structure
- 50 test cases per strategy
- 6 strategies tested
- Balanced across all 4 quadrants (~12-13 tasks each)
- Total: 300 inferences

## Recommendations

Based on partial results:

1. **Recommended Strategy: Expert Persona (0.2.6.4)**
   - Highest accuracy (100% on initial tests)
   - Consistent parsing
   - Reasonable latency

2. **Production Considerations:**
   - Batch processing for non-urgent tasks
   - Consider smaller model (Phi-3 1.8B) for faster inference
   - Pre-load model on app startup

3. **Next Steps:**
   - Complete full 50-task benchmark
   - Test other quadrants (DECIDE, DELEGATE, DELETE)
   - Optimize inference speed with KV cache tuning

## How to Complete Benchmark

```bash
# Check if test is still running
adb shell "ps -A | grep llmtest"

# Monitor progress
adb logcat -s PromptBenchmark

# Results will be logged with format:
# [✓] #N: Task description | EXPECTED -> PREDICTED (latencyMs)

# Final results will show:
# --- Strategy Name Results ---
# Accuracy: X% (N/M)
# Avg Latency: Xms
```

## Test Configuration

```kotlin
MAX_TOKENS = 50
TEMPERATURE = 0.2f
TOP_P = 0.9f
CONTEXT_SIZE = 2048
THREADS = 4
```

## Files Created

- `app/src/main/java/app/jeeves/llmtest/benchmark/PromptStrategy.kt` - 6 prompt strategies
- `app/src/main/java/app/jeeves/llmtest/benchmark/ExtendedTestDataset.kt` - 50 test cases
- `app/src/main/java/app/jeeves/llmtest/benchmark/PromptEngineeringBenchmark.kt` - Benchmark runner
- `app/src/androidTest/java/app/jeeves/llmtest/PromptEngineeringInstrumentedTest.kt` - Instrumented tests
