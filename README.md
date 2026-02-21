# SightLine

**SightLine** is a classroom-wide live captioning system designed to provide consistent, hands-free visual access to spoken instruction for students with hearing impairments or attention difficulties.

> **Note:** This repository **does not include the speech recognition models** due to their size (~1GB). Users must download and install the required models into the `/models/` directory to use the system.

## Core Concept

SightLine uses a dedicated microphone worn or clipped to the teacher to capture clear classroom audio and convert it into **real-time captions**. These captions are displayed on a **desk-mounted, adjustable mini monitor**, keeping text always within the student’s natural line of sight.  

Additionally, captions are **stored** and can be converted into **structured notes** or **AI-assisted summaries** within the SightLine App/Program.

## Features

- Real-time live captioning of classroom speech.
- Desk-mounted mini monitor keeps captions in the student's line of sight.
- Captured captions can be saved for later review.
- Supports structured note generation and AI-assisted summarization.
- Designed for students with hearing impairments or attention difficulties.

## How It Works

1. **Audio Capture**: A teacher wears or clips a dedicated microphone.
2. **Speech-to-Text**: Audio is converted into text captions in real-time.
3. **Display**: Captions appear instantly on the student’s monitor.
4. **Storage & Processing**: Captions are saved and can be transformed into notes or summaries through the SightLine App.

## Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/big-bruce-22/sightline.git
   cd sightline
