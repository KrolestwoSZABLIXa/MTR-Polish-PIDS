# MTR Polish Pack 🇵🇱

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.11+-blue.svg)](https://fabricmc.net/)
[![MTR](https://img.shields.io/badge/MTR-4.0.5+-orange.svg)](https://modrinth.com/mod/minecraft-transit-railway)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**MTR Polish Pack** to kompleksowy dodatek (*addon mod*) dla **Minecraft Transit Railway (MTR 4.0.5+)** na platformę **Fabric 1.20.1**, wprowadzający do gry realistyczne, wiernie odwzorowane elementy polskiej infrastruktury kolejowej (PKP PLK / CSDIP) oraz drogowej.

---

## 🚆 Funkcjonalności

### 1. Tablica krawędziowa PKP (CSDIP / Ipi-1):
* **Autentyczna kolorystyka PKP PLK**: Głęboki granat wyświetlacza `#000a64` (RAL 5002), zrównoważona biel napisów oraz charakterystyczny czerwony pasek opóźnienia.
* **Działający zegar analogowy PKP**:
  * Oryginalna dworcowa tarcza PKP z indeksami minutowymi i godzinowymi.
  * **Płynnie poruszające się wskazówki w czasie rzeczywistym** zsynchronizowane z czasem lokalnym.
  * Wskazówki renderowane bezpośrednio na dynamicznej teksturze tarczy — **gwarancja 100% widoczności pod każdym kątem i na wszystkich shaderach** (Iris / OptiFine).
  * Tablica jest dwustronna z możliwością natychmiastowej zmiany strony zegara (w stronę toru / peronu) poprzez **Shift + PPM pędzlem MTR (Brush)** lub z poziomu menu GUI.
* **Biały pasek relacji pociągu (Kategoria / Linia / Numer)**:
  * Stały, podświetlany biały prostokąt pod godziną odjazdu.
  * Krótkie oznaczenia (np. `KM`, `SKA3`, `Os 33417`) są elegancko wycentrowane.
  * Długie nazwy (np. `IC 13100 MALINOWSKI`) płynnie przewijają się w pętli (*marquee*), ściśle wewnątrz białego paska bez wyjeżdżania na granatowe tło.
* **Rozkład i opóźnienia**:
  * **Planowy czas odjazdu (`HH:mm`)**: Pozostaje stały zgodnie z rozkładem jazdy (nie przesuwa się sztucznie przy opóźnieniach).
  * **Stacje pośrednie („Przez / Via”)**: Automatyczne stronicowanie lub płynny bieg kolejnych stacji na trasie.
  * **Pasek opóźnienia**: Płynny, jednorazowy przelot paska informacyjnego na czerwonym tle z informacją o opóźnieniu oraz wiadomościami niestandardowymi.
  * **Oznaczenie peronu**: Cechowanie obudowy napisem `peron X / platform` w czcionce Myriad Pro z automatycznym wyciąganiem cyfry (np. z `1 tor 1` generuje `1`).

---

### 2. Tablica miejscowości (Znak drogowy E-17a / E-18a):
* **Oryginalna czcionka Drogowskaz**: Zdigitalizowany polski krój pisma drogowego bez spłaszczania ani zniekształcania znaków.
* **Dynamiczna szerokość tablicy**: Tablica proceduralnie i proporcjonalnie rozszerza się w świecie gry w oparciu o długość nazwy miejscowości (od `Hel` aż po `Konstantynów Łódzki`).
* **Tryb znaku E-18a (Koniec miejscowości)**: Opcja przekreślenia czerwoną linią diagonalną.
* **Konstrukcja stalowa**: Słupy ze stali ocynkowanej z ceownikami montażowymi i obejmami z tyłu tablicy.
* **Wygodna konfiguracja MTR Brushem**:
  * **PPM** pędzlem otwiera menu konfiguracji nazwy i trybu tablicy.
  * **Shift + PPM** natychmiast przełącza tablicę między E-17a (początek) a E-18a (koniec miejscowości).

---

### 3. Zaawansowana typografia Java2D:
* Brak pikselozy i deformacji domyślnych fontów Minecrafta — teksty generowane są dynamicznie przez silnik typograficzny Java2D z pełnym antyaliasingiem subpikselowym i kerningiem HarfBuzz:
  * **Roboto Medium / Regular / Italic** — czcionki systemu PKP CSDIP.
  * **Myriad Pro Regular / Italic** — oznaczenia peronowe Ipi-1.
  * **Drogowskaz** — polskie tablice drogowe.

---

### 4. Pełna kompatybilność z shaderami (Iris / OptiFine):
* Automatyczne wykrywanie aktywnych shaderów (`IrisApi` / `optifine.Config`).
* Dedykowana warstwa emisyjna zapobiegająca oślepiającym rozbłyskom (*bloom flare*), zachowująca idealny odcień granatu `#000a64` oraz czystą biel w nocy.
* Architektura bez nakładających się poligonów (zero Z-fightingu, brak znikania elementów pod ostrymi kątami widzenia).

---

## 🛠️ Wymagania

* **Minecraft**: `1.20.1`
* **Fabric Loader**: `>= 0.15.0`
* **Fabric API**
* **Minecraft Transit Railway (MTR)**: `>= 4.0.5`

---

## 📦 Budowanie ze źródeł

Wymagany jest **JDK 17**:

```bash
# Sklonowanie repozytorium
git clone https://github.com/KrolestwoSZABLIXa/MTR-Polish-Pack.git

# Zbudowanie projektu
cd MTR-Polish-Pack
./gradlew build
```

Zbudowany plik `.jar` (`mtr-polish-pack-1.0.0.jar`) pojawi się w katalogu `build/libs/`.

---

## 📄 Licencja

Projekt objęty jest licencją [MIT](LICENSE).
Twórca: **SZABLIX**