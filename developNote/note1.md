# Development Note 1: Using Agentic Code Assistant for Video Trimmer Project

## Overview
This note documents my experience using an AI-powered code assistant to develop a cross-platform video trimmer application using Kotlin Multiplatform (KMP). The journey involved several challenges and learning experiences that highlight both the capabilities and limitations of current AI coding assistants.

## 1. Initial Technical Stack Evaluation

**Approach**: I started by asking the AI to help evaluate the technical stack by describing:
- The problem I wanted to solve (video trimming application)
- My preferred tools and technologies
- Potential experiments and research needed
- Project milestones
- Generate a comprehensive README.md

**Outcome**: This phase went relatively well. The AI was able to provide a structured approach to the project and generated a reasonable README.md that outlined the project scope and technical requirements.

## 2. The "Hello World" Challenge

**Problem**: After completing the README, I asked the AI to start working on milestone 1. However, the AI generated far too much code before we could establish a simple "Hello World" cross-platform project.

**Issues Encountered**:
- Despite explicitly instructing the AI to focus on a minimal "Hello World" implementation, it failed to keep the scope small
- The generated project was not compilable
- The codebase became difficult to debug and fix due to its complexity

**Lesson Learned**: AI assistants tend to over-engineer solutions and struggle with incremental development approaches. They often jump to complex implementations rather than building foundational components first.

## 3. Ground-Up Approach with KMP Plugin

**Solution**: I decided to build the project from scratch using the default wizard provided by the Kotlin Multiplatform (KMP) plugin.

**Results**: 
- This approach proved to be much cleaner and more stable
- The generated boilerplate was minimal and functional
- Provided a solid foundation for incremental development

**Key Insight**: Sometimes the simplest, most conventional approach (using official tooling) is more reliable than AI-generated solutions.

## 4. Cross-Platform Build Challenges

**Challenge**: Building the project for Windows while developing on macOS.

**Initial Approach**: I asked the AI how to build an executable file for Windows. The AI provided a Gradle command that seemed promising and insisted that I should see a `.msi` file after building.

**The Problem**: 
- No `.msi` file was generated despite following the AI's instructions
- The AI was confident that I had made a mistake and kept insisting the approach was correct
- This led to significant time waste trying to troubleshoot a non-existent issue

**Root Cause Discovery**: The fundamental issue was that **you cannot build Windows executables from macOS**. The executable can only be built on the target Windows OS.

**Lesson Learned**: AI assistants can be overly confident about incorrect information, especially regarding platform-specific build processes. Always verify platform limitations independently.

## 5. Video Rendering Implementation Challenges

**Initial AI Solution**: The AI's first approach was a frame-by-frame solution that generated individual images for every frame.

**Problems with This Approach**:
- Not performant for video playback
- Resource-intensive and inefficient

**Alternative Attempts**:
- **Native approach**: Failed to implement properly
- **Java library integration**: Also failed due to cross-platform compatibility issues
- **Native video player**: AI suggested opening an external video player, which was unacceptable for a video editing application

**Final Decision**: After multiple failed attempts, I accepted the frame-by-frame solution, rationalizing that the individual frames would be needed anyway for machine learning analysis of human poses.

**Lesson Learned**: Video rendering in cross-platform applications is complex, and AI assistants may not fully understand the performance implications of their suggested solutions.

## 6. Code Organization and Platform-Specific Logic

**Problem**: After extensive pair programming with the AI, the codebase became messy and poorly organized.

**Specific Issues**:
- AI placed Jetpack Compose code in platform-specific folders (`desktopMain`) instead of common code
- This violated KMP best practices where UI code should typically be in `commonMain`
- When asked to refactor, the AI failed to complete the task properly
- The refactoring process created more complexity rather than simplifying the code

**Root Cause**: The AI lacks a clear understanding of what code belongs in `commonMain` versus platform-specific folders like `desktopMain`.

**Key Insight**: AI assistants struggle with architectural decisions and code organization principles, especially in multi-platform projects where the separation of concerns is critical.

## Conclusions and Recommendations

### What AI Assistants Do Well:
- Initial project planning and documentation
- Generating boilerplate code and project structure
- Providing multiple solution approaches

### Where AI Assistants Struggle:
- **Incremental development**: Tendency to over-engineer simple requirements
- **Platform-specific knowledge**: Incorrect assumptions about cross-platform capabilities
- **Performance considerations**: May suggest inefficient solutions without considering implications
- **Code organization**: Poor understanding of architectural boundaries in multi-platform projects
- **Confidence calibration**: Being overly confident about incorrect information

### Best Practices for Working with AI Assistants:
1. **Start simple**: Always insist on minimal viable implementations first
2. **Verify platform limitations**: Don't rely solely on AI for platform-specific build processes
3. **Maintain architectural control**: Be explicit about code organization principles
4. **Question performance implications**: Always evaluate the efficiency of suggested solutions
5. **Use official tooling**: Prefer established tools and wizards over AI-generated alternatives when available

### Final Thoughts
While AI assistants can be valuable for certain aspects of development, they require careful supervision and should not be trusted blindly, especially for architectural decisions and platform-specific implementations. The most successful approach combines AI assistance with human oversight and conventional development practices.