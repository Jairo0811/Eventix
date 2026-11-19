#!/usr/bin/env python3
"""Compone la banda sonora original del tráiler de Eventix.

Todo el audio se sintetiza aquí, muestra a muestra. No se usa ninguna
grabación, canción ni biblioteca de efectos de terceros, así que el resultado
es original y puede publicarse en redes y portafolio sin restricciones de
licencia.

La pieza es electrónica cinematográfica con progresión emocional: arranca en
penumbra, abre en la escena de descubrimiento, gana cuerpo en la de gestión,
alcanza su punto alto en el montaje y cierra con un apagado suave.

Uso:
    python3 scripts/make-audio.py
Genera en assets/audio/:
    trailer-50s.wav   banda sonora del tráiler (50 s)
    teaser-15s.wav    banda sonora del teaser (15 s)
"""
from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 48_000
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "audio"

# La pieza vive en La menor: sobrio, tecnológico, sin resultar triste.
A2, C3, E3, A3, C4, E4, G4, A4, B4, D5, E5 = (
    110.00, 130.81, 164.81, 220.00, 261.63, 329.63,
    392.00, 440.00, 493.88, 587.33, 659.25,
)


def envelope(t: float, duration: float, attack: float, release: float) -> float:
    """Envolvente de amplitud con ataque y caída suaves."""
    if t < 0 or t > duration:
        return 0.0
    if t < attack:
        return t / attack
    if t > duration - release:
        return max(0.0, (duration - t) / release)
    return 1.0


def pluck(buf: list[float], start: float, freq: float, duration: float,
          gain: float, detune: float = 0.0) -> None:
    """Nota de sintetizador con armónicos, al estilo de un pluck electrónico."""
    begin = int(start * SAMPLE_RATE)
    total = int(duration * SAMPLE_RATE)
    for i in range(total):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        # Caída exponencial: cuerpo corto y cola larga.
        decay = math.exp(-3.2 * t / duration)
        env = envelope(t, duration, 0.008, duration * 0.55) * decay
        phase = 2 * math.pi * (freq + detune) * t
        sample = (
            math.sin(phase)
            + 0.38 * math.sin(2 * phase)
            + 0.16 * math.sin(3 * phase)
            + 0.07 * math.sin(4 * phase)
        )
        buf[index] += sample * env * gain


def sub_bass(buf: list[float], start: float, freq: float, duration: float,
             gain: float) -> None:
    """Bajo profundo que sostiene la escena sin competir con la voz."""
    begin = int(start * SAMPLE_RATE)
    total = int(duration * SAMPLE_RATE)
    for i in range(total):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        env = envelope(t, duration, 0.05, 0.25)
        phase = 2 * math.pi * freq * t
        buf[index] += (math.sin(phase) + 0.22 * math.sin(2 * phase)) * env * gain


def pad(buf: list[float], start: float, freqs: list[float], duration: float,
        gain: float) -> None:
    """Colchón armónico con ligero batido entre voces, para dar amplitud."""
    begin = int(start * SAMPLE_RATE)
    total = int(duration * SAMPLE_RATE)
    for i in range(total):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        env = envelope(t, duration, duration * 0.18, duration * 0.3)
        # Movimiento lento del timbre: evita que el colchón suene estático.
        shimmer = 1.0 + 0.04 * math.sin(2 * math.pi * 0.17 * t)
        acc = 0.0
        for voice, freq in enumerate(freqs):
            detune = 0.6 * (voice - len(freqs) / 2)
            acc += math.sin(2 * math.pi * (freq + detune) * t * shimmer)
        buf[index] += (acc / len(freqs)) * env * gain


def noise_sweep(buf: list[float], start: float, duration: float, gain: float,
                rising: bool = True) -> None:
    """Whoosh: ruido filtrado que barre, para las transiciones de luz."""
    begin = int(start * SAMPLE_RATE)
    total = int(duration * SAMPLE_RATE)
    # Ruido rosa aproximado por integración de ruido blanco.
    state = 0.0
    seed = 12345
    for i in range(total):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        progress = t / duration
        seed = (1103515245 * seed + 12345) & 0x7FFFFFFF
        white = (seed / 0x3FFFFFFF) - 1.0
        # Filtro pasa-bajos de un polo cuya frecuencia de corte barre.
        cutoff = 0.02 + (0.5 * progress if rising else 0.5 * (1 - progress))
        state += cutoff * (white - state)
        env = math.sin(math.pi * progress) ** 1.6
        buf[index] += state * env * gain


def impact(buf: list[float], start: float, gain: float) -> None:
    """Impacto digital: golpe grave con un transitorio brillante."""
    duration = 1.4
    begin = int(start * SAMPLE_RATE)
    total = int(duration * SAMPLE_RATE)
    seed = 987_654
    for i in range(total):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        # Barrido descendente de frecuencia: la firma del impacto cinematográfico.
        freq = 150 * math.exp(-7 * t) + 34
        body = math.sin(2 * math.pi * freq * t) * math.exp(-3.6 * t)
        seed = (1103515245 * seed + 12345) & 0x7FFFFFFF
        white = (seed / 0x3FFFFFFF) - 1.0
        crack = white * math.exp(-48 * t) * 0.3
        buf[index] += (body + crack) * gain


def blip(buf: list[float], start: float, freq: float, gain: float) -> None:
    """Pulsación de interfaz: muy corta, para los toques sobre la UI."""
    duration = 0.1
    begin = int(start * SAMPLE_RATE)
    for i in range(int(duration * SAMPLE_RATE)):
        index = begin + i
        if index >= len(buf):
            break
        t = i / SAMPLE_RATE
        env = math.exp(-26 * t)
        buf[index] += math.sin(2 * math.pi * freq * t) * env * gain


def pulse_grid(buf: list[float], start: float, end: float, bpm: float,
               gain: float) -> None:
    """Pulso rítmico sobrio que da energía sin saturar la mezcla."""
    beat = 60.0 / bpm
    position = start
    step = 0
    while position < end:
        # Acento en el primer tiempo de cada compás.
        accent = 1.0 if step % 4 == 0 else 0.55
        duration = 0.16
        begin = int(position * SAMPLE_RATE)
        for i in range(int(duration * SAMPLE_RATE)):
            index = begin + i
            if index >= len(buf):
                break
            t = i / SAMPLE_RATE
            env = math.exp(-20 * t)
            freq = 62 * math.exp(-11 * t) + 44
            buf[index] += math.sin(2 * math.pi * freq * t) * env * gain * accent
        position += beat / 2
        step += 1


def normalize(buf: list[float], peak: float = 0.89) -> None:
    """Normaliza con un limitador suave, para que nada recorte."""
    highest = max((abs(sample) for sample in buf), default=0.0)
    if highest == 0:
        return
    scale = peak / highest
    for i, sample in enumerate(buf):
        value = sample * scale
        # Saturación suave: redondea los picos en vez de cortarlos.
        buf[i] = math.tanh(value * 1.08) * 0.96


def fade_edges(buf: list[float], fade_in: float = 0.05,
               fade_out: float = 1.1) -> None:
    """Evita chasquidos al principio y cierra con un apagado elegante."""
    n_in = int(fade_in * SAMPLE_RATE)
    for i in range(min(n_in, len(buf))):
        buf[i] *= i / n_in
    n_out = int(fade_out * SAMPLE_RATE)
    for i in range(min(n_out, len(buf))):
        buf[len(buf) - 1 - i] *= i / n_out


def write_wav(path: Path, buf: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    frames = bytearray()
    for sample in buf:
        value = max(-1.0, min(1.0, sample))
        frames += struct.pack("<h", int(value * 32_767))
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(SAMPLE_RATE)
        out.writeframes(bytes(frames))
    seconds = len(buf) / SAMPLE_RATE
    print(f"  {path.name}  {seconds:.2f} s  {path.stat().st_size // 1024} KB")


def compose_trailer() -> list[float]:
    """Banda sonora de 50 s, alineada con las seis escenas del storyboard."""
    duration = 50.0
    buf = [0.0] * int(duration * SAMPLE_RATE)

    # --- Escena 1 (00:00-00:08) El inicio: penumbra que se abre ---
    pad(buf, 0.0, [A2, E3, A3], 9.0, 0.30)
    impact(buf, 0.35, 0.52)
    blip(buf, 2.4, E5, 0.16)
    blip(buf, 2.75, B4, 0.13)
    # El logo se forma hacia 4.5 s: un impacto suave lo acompaña.
    impact(buf, 4.5, 0.34)
    pluck(buf, 5.0, A4, 1.6, 0.17)
    pluck(buf, 5.5, E4, 1.6, 0.14)
    noise_sweep(buf, 6.9, 1.4, 0.26, rising=True)

    # --- Escena 2 (00:08-00:17) Descubre: entra el pulso ---
    sub_bass(buf, 8.0, A2, 9.2, 0.30)
    pad(buf, 8.0, [C4, E4, A4], 9.2, 0.21)
    pulse_grid(buf, 8.2, 17.0, 104, 0.30)
    for offset, note in enumerate([A4, C4, E4, A4, B4, E4]):
        pluck(buf, 8.4 + offset * 0.72, note, 0.85, 0.15)
    blip(buf, 10.1, G4, 0.12)
    blip(buf, 12.3, E5, 0.12)
    noise_sweep(buf, 16.2, 1.0, 0.22, rising=True)

    # --- Escena 3 (00:17-00:27) Organiza: más cuerpo armónico ---
    sub_bass(buf, 17.0, C3, 10.2, 0.31)
    pad(buf, 17.0, [E4, G4, C4], 10.2, 0.24)
    pulse_grid(buf, 17.0, 27.0, 104, 0.33)
    for offset, note in enumerate([C4, E4, G4, E4, C4, G4, A4]):
        pluck(buf, 17.3 + offset * 0.68, note, 0.9, 0.155)
    blip(buf, 19.0, B4, 0.11)
    blip(buf, 21.4, D5, 0.11)
    blip(buf, 23.8, G4, 0.11)
    noise_sweep(buf, 26.1, 1.0, 0.23, rising=True)

    # --- Escena 4 (00:27-00:37) Tu entrada digital: foco y detalle ---
    sub_bass(buf, 27.0, E3, 10.2, 0.29)
    pad(buf, 27.0, [A4, C4, E5], 10.2, 0.23)
    pulse_grid(buf, 27.2, 37.0, 104, 0.28)
    for offset, note in enumerate([E4, A4, B4, E5, B4, A4]):
        pluck(buf, 27.5 + offset * 0.78, note, 1.0, 0.16)
    # El QR se valida hacia 33.5 s: dos pulsaciones y un impacto discreto.
    blip(buf, 33.2, E5, 0.17)
    blip(buf, 33.45, B4, 0.15)
    impact(buf, 33.7, 0.26)
    noise_sweep(buf, 36.1, 1.0, 0.25, rising=True)

    # --- Escena 5 (00:37-00:44) Conecta: punto alto del montaje ---
    sub_bass(buf, 37.0, A2, 7.4, 0.34)
    pad(buf, 37.0, [C4, E4, A4, E5], 7.4, 0.27)
    pulse_grid(buf, 37.0, 44.0, 112, 0.36)
    for offset, note in enumerate([A4, E5, D5, C4, E4, A4, B4, E5]):
        pluck(buf, 37.2 + offset * 0.42, note, 0.6, 0.165)
    # Riser que desemboca en el cierre.
    noise_sweep(buf, 42.4, 1.7, 0.34, rising=True)

    # --- Escena 6 (00:44-00:50) Cierre: impacto y apagado ---
    impact(buf, 44.1, 0.62)
    sub_bass(buf, 44.1, A2, 5.6, 0.30)
    pad(buf, 44.2, [A3, C4, E4], 5.6, 0.26)
    pluck(buf, 44.3, A4, 2.6, 0.18)
    pluck(buf, 44.9, E5, 2.4, 0.14)
    noise_sweep(buf, 47.6, 2.2, 0.16, rising=False)

    normalize(buf)
    fade_edges(buf, 0.04, 1.3)
    return buf


def compose_teaser() -> list[float]:
    """Banda sonora de 15 s: la misma identidad, con un corte más directo."""
    duration = 15.0
    buf = [0.0] * int(duration * SAMPLE_RATE)

    # 00:00-00:03 Logo con impacto de luz.
    impact(buf, 0.18, 0.60)
    pad(buf, 0.0, [A2, E3, A3], 3.6, 0.30)
    pluck(buf, 1.3, A4, 1.3, 0.17)
    noise_sweep(buf, 2.4, 0.8, 0.28, rising=True)

    # 00:03-00:07 Descubrimiento de eventos.
    sub_bass(buf, 3.0, A2, 4.3, 0.31)
    pad(buf, 3.0, [C4, E4, A4], 4.3, 0.22)
    pulse_grid(buf, 3.0, 7.2, 112, 0.33)
    for offset, note in enumerate([A4, C4, E4, B4]):
        pluck(buf, 3.2 + offset * 0.5, note, 0.62, 0.16)

    # 00:07-00:11 Boleta, QR y dashboard.
    sub_bass(buf, 7.0, E3, 4.3, 0.30)
    pad(buf, 7.0, [E4, G4, B4], 4.3, 0.23)
    pulse_grid(buf, 7.0, 11.2, 112, 0.34)
    for offset, note in enumerate([E4, B4, E5, G4]):
        pluck(buf, 7.2 + offset * 0.5, note, 0.62, 0.16)
    blip(buf, 9.1, E5, 0.16)
    impact(buf, 9.35, 0.24)
    noise_sweep(buf, 10.3, 0.9, 0.3, rising=True)

    # 00:11-00:15 Logo y eslogan.
    impact(buf, 11.2, 0.58)
    sub_bass(buf, 11.2, A2, 3.8, 0.29)
    pad(buf, 11.3, [A3, C4, E4], 3.7, 0.26)
    pluck(buf, 11.4, A4, 2.2, 0.18)
    pluck(buf, 11.9, E5, 2.0, 0.14)
    noise_sweep(buf, 13.4, 1.5, 0.15, rising=False)

    normalize(buf)
    fade_edges(buf, 0.03, 1.0)
    return buf


if __name__ == "__main__":
    print("Sintetizando banda sonora original de Eventix...")
    write_wav(OUT / "trailer-50s.wav", compose_trailer())
    write_wav(OUT / "teaser-15s.wav", compose_teaser())
    print("Audio original, sin material de terceros.")
