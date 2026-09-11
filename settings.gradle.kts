// Bridge to allow Android Studio to see the project from the root
include(":app")
project(":app").projectDir = file("android/app")
