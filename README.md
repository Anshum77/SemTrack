# SemTrack

SemTrack is a personal college companion Android app focused on task management, attendance tracking, and evaluation calculations.

## Current Progress

- Tasks module is fully persistent with Room, ViewModel, and StateFlow.
- Attendance module now has course management plus a course detail screen for date-wise attendance marking and history.
- Evaluations module is still scaffolded for future implementation.

## Implemented Features

### Tasks

- Multiple task lists with swipe navigation
- Add, rename, and delete lists
- Add, edit, star, complete, restore, and reorder tasks
- Persisted local storage with Room database
- Collapsible completed section and delete-completed flow

### Attendance

- Create, rename, and delete courses
- Room-backed course dashboard cards
- Course detail screen with date-wise Present/Absent marking
- Duplicate attendance protection and change confirmation
- Attendance history in a bottom sheet
- Live attendance statistics from Room records

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
