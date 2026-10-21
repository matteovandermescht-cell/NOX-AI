# NOX AI

A clean, dark Android chat client built with Kotlin and Jetpack Compose. It connects to a configurable OpenAI-compatible Chat Completions endpoint.

## Features

- Dark, minimal chat interface
- Multi-message conversation context
- New-chat control and loading/error states
- Configurable HTTPS endpoint, model, and API key
- GitHub Actions build that publishes a downloadable debug APK

## Build the APK

Open **Actions → Build NOX AI APK** and run the workflow, or push a commit to `main`. When the run succeeds, open the run and download the `NOX-AI-debug-apk` artifact.

## Configure

Open Settings in the app and enter an HTTPS API endpoint, model name, and API key. The default endpoint is the OpenAI-compatible Chat Completions endpoint. Do not commit API keys to this repository.

## Scope

NOX AI is an Android client, not a separately trained foundation model. Available capabilities depend on the configured API provider. The first version does not yet include streaming responses, persistent chat history, file uploads, voice, or image input.
