# TableMorph - Wavetable Generator for Vital

TableMorph is a powerful wavetable generator designed specifically for the [Vital synthesizer](https://vital.audio/). It allows you to create unique wavetables from scratch or morph them with your own audio samples.

![TableMorph Logo](docs/images/tablemorph_logo.png)

## Features

- **Random Wavetable Generation**: Create unique wavetables with various harmonic structures
- **Sample Morphing**: Blend your audio samples with generated wavetables
- **Batch Processing**: Generate multiple wavetables at once
- **Direct Vital Integration**: Save wavetables directly to Vital's wavetable directory
- **Customizable Settings**: Configure frame count, sample count, and morphing parameters

## Requirements

- Java 8 or higher
- Vital synthesizer (for using the generated wavetables)

## Installation

1. Download the latest release from the [Releases](https://github.com/proxeeus/tablemorph/releases) page
2. Extract the ZIP file to a location of your choice
3. Run the application using the provided launcher script:
   - On Windows: `run-tablemorph.bat`
   - On macOS/Linux: `./run-tablemorph.sh`

## Quick Start

1. Launch TableMorph
2. Choose an option from the main menu:
   - Generate a single wavetable
   - Morph with samples
   - Batch generate wavetables
   - Batch morph wavetables
3. Generated wavetables will be saved to:
   - The local `wavetables` and `morphs` directories
   - Your Vital wavetables directory (if enabled)

## Using Your Wavetables in Vital

The generated wavetables are automatically saved to your Vital wavetables directory (if enabled in settings). To use them:

1. Open Vital
2. Click on an oscillator
3. Click the wavetable button
4. Navigate to the User Wavetables section
5. Select your generated wavetable

## Adding Your Own Samples

To use your own audio samples for morphing:

1. Place WAV files in the `sounds` directory
2. Choose the "Morph with Samples" or "Batch Morph Wavetables" option
3. The application will randomly select and incorporate your samples

## Configuration

TableMorph offers several configuration options:

- **Wavetable Settings**: Frame count, sample count
- **Morphing Settings**: Max morph samples, full sample probability
- **Vital Integration**: Enable/disable saving to Vital, customize Vital directory path

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/tablemorph.git
cd tablemorph

# Build with Maven
./mvnw clean install

# Run the application
./run-tablemorph.sh  # or run-tablemorph.bat on Windows
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Vital Synthesizer](https://vital.audio/) by Matt Tytel
- All sample contributors (if using Creative Commons samples)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. 