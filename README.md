# MTR Polish PIDS

**MTR Polish PIDS** to dodatek (addon mod) dla **Minecraft Transit Railway (MTR 4.0.5+)** na platformę **Fabric 1.20.1**, wprowadzający do gry realistyczne polskie tablice Systemu Dynamicznej Informacji Pasażerskiej (CSDIP / PKP PLK).

## 🚆 Funkcjonalności

* **Tablica krawędziowa PKP**:
  * Wiernie odwzorowany układ graficzny polskich wyświetlaczy peronowych (charakterystyczna granatowa obudowa, ciemnoniebieskie tło, belka peronowa, czerwony pasek akcentowy).
  * Wyświetlanie stacji docelowej, kategorii i numeru pociągu / linii, odliczania czasu do przyjazdu (minuty/sekundy) oraz listy stacji pośrednich („Przez / Via”) z automatycznym stronicowaniem.
* **Działający zegar analogowy PKP**:
  * Prawdziwa tarcza dworcowa z indeksami minutowo-godzinowymi zamiast statycznej atrapy.
  * Poruszające się wskazówki godzinowa i minutowa zsynchronizowane z czasem świata gry.
* **Inteligentna orientacja zegara & dwustronność**:
  * Tablica jest dwustronna – zegar na stronie przeciwnej automatycznie renderuje się na tej samej fizycznej krawędzi obudowy.
  * Możliwość natychmiastowej zmiany strony zegara (w stronę toru / peronu) poprzez **Shift + prawy klik MTR Brushem** (pędzlem).
* **Pełna integracja z MTR**:
  * Kliknięcie **MTR Brushem** otwiera natywne okno konfiguracji PIDS (`PIDSConfigScreen`), pozwalające przypisać konkretny peron, filtrować przyjazdy oraz ustawiać własne komunikaty.

## 🛠️ Wymagania

* Minecraft `1.20.1`
* Fabric Loader `>= 0.15.0`
* Fabric API
* Minecraft Transit Railway (MTR) `>= 4.0.5`

## 📦 Budowanie ze źródeł

Wymagany jest JDK 17:
```bash
./gradlew build
```
Zbudowany plik `.jar` pojawi się w katalogu `build/libs/`.