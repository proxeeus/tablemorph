# TableMorph - Wavetable Generator for Vital

TableMorph is a powerful wavetable generator designed specifically for the [Vital synthesizer](https://vital.audio/). It allows you to create unique wavetables from scratch, generate single-cycle waveforms, or morph them with your own audio samples.

![TableMorph Logo](docs/images/tablemorph_logo.png)

## Features

- **Advanced Wavetable Generation**: Create complex wavetables with rich harmonic content and unique timbres
- **High-Variability Morphing Algorithms**: Five distinct morphing techniques with dynamic randomization for truly unique results
- **Single-Cycle Wavetables**: Generate oscillator waveforms with various types (sine, saw, square, triangle, FM, additive, formant)
- **Sample Morphing**: Blend your audio samples with generated wavetables using advanced spectral processing
- **Multi-Synthesis Techniques**: Utilizes additive, FM, phase distortion, waveshaping and spectral synthesis methods
- **Batch Processing**: Generate multiple wavetables or single-cycle waveforms at once
- **Direct Vital Integration**: Save wavetables directly to Vital's wavetable directory
- **Customizable Settings**: Configure frame count, sample count, and morphing parameters

## Morphing Algorithms

TableMorph features five sophisticated morphing algorithms:

- **Simple Blending**: Non-linear blending with dynamic intensity and phase modulation
- **Additive**: Selective harmonic processing with independent phase manipulation
- **Harmonic**: Frequency-dependent structural modifications based on sample characteristics
- **Wave Folding**: Adaptive waveshaping with multiple folding thresholds and saturation
- **Spectral**: Multi-band frequency transformations with independent processing per band

Each algorithm ensures unique results even with similar input parameters through multiple layers of controlled randomization.

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
   - Generate a single-cycle wavetable
   - Morph with samples
   - Batch generate wavetables
   - Batch generate single-cycle wavetables
   - Batch morph wavetables
3. Generated files will be saved to:
   - Multi-frame wavetables: `wavetables` directory
   - Single-cycle wavetables: `singlecycles` directory
   - Morphed wavetables: `morphs` directory
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

## Single-Cycle Wavetable Types

TableMorph can generate several types of single-cycle wavetables:

- **Sine**: Pure sine waves with optional harmonic content
- **Triangle**: Triangle waves with variable slope
- **Sawtooth**: Standard and reverse sawtooth waves
- **Square**: Square waves with variable pulse width
- **Noise**: Filtered noise for usable wavetables
- **FM**: Frequency modulation with randomized carrier and modulator parameters
- **Additive**: Harmonically rich waveforms with multiple partials
- **Formant**: Vowel-like waveforms with formant filtering

Each generated waveform includes randomized parameters to ensure uniqueness.

## Configuration

TableMorph offers several configuration options:

- **Wavetable Settings**: Frame count, sample count
- **Single-Cycle Settings**: Sample count
- **Morphing Settings**: Max morph samples, full sample probability
- **Vital Integration**: Enable/disable saving to Vital, customize Vital directory path

## Building from Source

```bash
# Clone the repository
git clone https://github.com/proxeeus/tablemorph.git
cd tablemorph

# Build with Maven
./mvnw clean package

# Run the application
java -jar target/tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar
# Or use the provided scripts:
# ./run-tablemorph.sh (macOS/Linux)
# run-tablemorph.bat (Windows)
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Vital Synthesizer](https://vital.audio/) by Matt Tytel
- All sample contributors (if using Creative Commons samples)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. 