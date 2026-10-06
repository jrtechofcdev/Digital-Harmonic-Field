#!/bin/bash
# Roda o detector REAL do app (classes Kotlin compiladas) sobre uma pasta de WAVs.
# Antes: gradle :app:compileDebugUnitTestKotlin :app:processDebugJavaRes
# Uso: tools/key-model/runbench.sh <pasta_wav> <saida.csv> [notas.jsonl] [parte] [partes]
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
I="$ROOT/app/build/intermediates"
STDLIB=$(find ~/.gradle/caches/modules-2 -name 'kotlin-stdlib-2.*.jar' | sort | tail -1)
CP="$I/java_res/debug/processDebugJavaRes/out:$I/built_in_kotlinc/debug/compileDebugKotlin/classes:$I/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes:$STDLIB"
java -cp "$CP" com.example.bench.KeyBench "$@"
