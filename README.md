# Compiler Construction 1 Project / Projekat Iz Programskih Prevodioca 1

This project implements a **compiler for the imaginary Microjava programming language**, designed to translate valid Mikrojava programs into bytecode executable on the Mikrojava Virtual Machine (MJVM). 
The compiler includes four primary phases: 
- lexical analysis
- syntax analysis
- semantic analysis
- code generation.

It is a project on the course Compiler Construction 1 ("Programski Prevodioci 1") on University of Belgrade, Faculty of Electrical Engineering, Module for Computer Engineering and Informatics

---

## Project Description

The goal is to build a **working compiler for Mikrojava**, which supports the following functionalities:

1. **Lexical Analysis**:
   - Processes source code to identify tokens such as identifiers, constants, keywords, operators, and comments.
   - Implemented using the **JFlex** with `.flex` specifications.
   - Detects and reports lexical errors with line and column numbers.

2. **Syntax Analysis**:
   - Verifies if the sequence of tokens forms grammatically correct statements.
   - Builds an **Abstract Syntax Tree (AST)** for valid programs using the **AST-CUP tool**.
   - Includes error recovery for certain syntactic issues, allowing continued parsing.

3. **Semantic Analysis**:
   - Checks the program for contextual errors like type mismatches, undeclared variables, and improper symbol usage.
   - Updates the **Symbol Table** during analysis.
   - Ensures all semantic rules of Mikrojava are satisfied.

4. **Code Generation**:
   - Translates syntax and semantically valid programs into bytecode for the MJVM.
   - Produces `.obj` files that can be executed in the virtual environment.

---

## Key Features

### 1. **Lexical Analysis**
- Detects and processes:
  - Identifiers, numeric and character constants.
  - Keywords (e.g., `if`, `else`, `while`, `return`).
  - Operators (`+`, `-`, `*`, `/`, `=`, etc.).
  - Single-line and multi-line comments.
- Ignores whitespace and formats (e.g., spaces, tabs).
- Reports errors for unrecognized symbols or incomplete constructs.

### 2. **Syntax Analysis**
- Implements an **LALR(1) grammar** for Mikrojava.
- Supports error recovery for common mistakes, such as:
  - Missing semicolons in variable declarations.
  - Incorrect assignment statements.
- Builds and prints the Abstract Syntax Tree (AST) for valid programs.

### 3. **Semantic Analysis**
- Updates the **Symbol Table** during parsing:
  - Tracks global and local variables, constants, arrays, and functions.
  - Checks for proper declaration and usage of all symbols.
- Validates context-sensitive rules, including:
  - Type consistency in expressions and assignments.
  - Correct parameter passing in function calls.

### 4. **Code Generation**
- Generates bytecode compatible with MJVM for:
  - Basic arithmetic and logical operations.
  - Array handling (declaration and indexing).
  - Conditional and loop constructs (`if`, `while`, `for`).
  - Input/output operations (`read`, `print`).
- Outputs an `.obj` file, which serves as the executable for MJVM.
