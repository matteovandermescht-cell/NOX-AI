# NOX AI

A clean, dark Android chat client built with Kotlin and Jetpack Compose. The app connects to a configurable OpenAI-compatible Chat Completions endpoint.

## Build the APK

A GitHub Actions workflow decodes the project archive and builds a debug APK. Open **Actions → Build NOX AI APK → Run workflow**. Download `NOX-AI-debug-apk` from the completed run.

## Configure

Open Settings in the app and enter an HTTPS API endpoint, model name, and API key. Do not commit API keys to this repository.

## Current scope

This is an Android chat client, not a separately trained foundation model. Available features depend on the configured API provider.