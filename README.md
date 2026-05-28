# SemTrack

SemTrack is a personal college companion Android app focused on task management, attendance tracking, and evaluation calculations.

## Current Progress

- Tasks module is fully persistent with Room, ViewModel, and StateFlow.
- Attendance module features persistent course management, date-wise history, and analytics.
- Evaluations module is fully implemented with hierarchical categories, exact double-precision scoring, and "best-of" dropping logic.

## Implemented Features

### Tasks

- Multiple task lists with swipe navigation
- Add, rename, and delete lists
- Add, edit, star, complete, restore, and reorder tasks
- Persisted local storage with Room database
- Collapsible completed section and delete-completed flow

### Attendance & Course Management

- Unified "Courses" dashboard backing both Attendance and Evaluations
- Drag-and-drop course reordering that perfectly syncs across modules
- Create, rename, and delete courses
- Course detail screen with date-wise Present/Absent marking
- Duplicate attendance protection and change confirmation
- Attendance history in a bottom sheet
- Live attendance statistics and safe skip tracking

### Evaluations

- Track overall obtained and evaluated percentage per course using strict `Double` precision calculation
- Create categorized assessment groups (e.g., "Quizzes", "Assignments", "Projects")
- Define group weightage and "Best of N" drop logic automatically
- Enter marks for individual tests and see real-time weighted contributions
- Distinct UI rendering for dropped assessments vs counted assessments

### Modern UI/UX

- Fully Material 3 compliant styling
- Sleek, customized Navigation Drawer with modern vector icons and premium header
- Unified bottom app bars and seamless Fragment transactions

## Tech Stack

- Kotlin
- Material 3 components
- Room database with KSP
- ViewModel + StateFlow
- Single-activity architecture with fragments
- ViewPager2 + TabLayoutMediator
- RecyclerView

## Database Notes

- Tasks and Attendance are stored in Room and survive app restarts.
- Attendance entries cascade-delete when a course is removed.
- The database currently uses migration support so existing data is preserved.

## Personal Use Notice

SemTrack is built primarily for personal academic and productivity use. It is an active app that is being improved incrementally rather than a showcase prototype.

## Getting Started

1. Open the project in Android Studio.
2. Sync Gradle files.
3. Build and run on an Android device or emulator.
