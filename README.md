# MTR Polish Pack

**MTR Polish Pack** to dodatek (addon mod) dla **Minecraft Transit Railway (MTR 4.0.5+)** na platformę **Fabric 1.20.1**, wprowadzający do gry realistyczne elementy polskiej infrastruktury kolejowej i drogowej.

## 🚆 Funkcjonalności

### 1. Tablica krawędziowa PKP (CSDIP / KZŁ):
* Wiernie odwzorowany układ graficzny polskich wyświetlaczy peronowych (charakterystyczna granatowa obudowa, ciemnoniebieskie tło, belka peronowa, czerwony pasek akcentowy opóźnień).
* Wyświetlanie stacji docelowej, kategorii i numeru pociągu / linii, odliczania czasu do przyjazdu oraz listy stacji pośrednich („Przez / Via”) z automatycznym stronicowaniem.
* Planowy czas odjazdu pozostający stały zgodnie z rozkładem jazdy (bez sztucznego przesuwania godziny przy opóźnieniach).
* Płynny, jednorazowy przelot paska opóźnienia na czerwonym tle bez zwielokrotniania napisów na ekranie.
* Wyciąganie właściwego numeru peronu (np. z „1 tor 1” lub „1.1” generowane jest czytelne „1”).
* **Działający zegar analogowy PKP**:
  * Prawdziwa tarcza dworcowa z indeksami minutowo-godzinowymi i poruszającymi się wskazówkami zsynchronizowanymi z czasem gry.
  * Tablica jest dwustronna z możliwością natychmiastowej zmiany strony zegara (w stronę toru / peronu) poprzez **Shift + prawy klik MTR Brushem** lub w menu konfiguracji.

### 2. Tablica miejscowości (Znak E-17a / E-18a):
* **Autentyczna czcionka Drogowskaz**: Oryginalny, zdigitalizowany polski krój pisma drogowego bez jakiegokolwiek zniekształcania czy spłaszczania liter.
* **Dynamiczna szerokość tablicy**: Tablica nie ściska czcionki przy długich nazwach miejscowości – zamiast tego cała płyta tablicy automatycznie i proporcjonalnie rozszerza się w świecie gry w oparciu o długość tekstu (np. od krótkich nazw jak „Hel” aż po długie jak „Konstantynów Łódzki”).
* **Znak E-18a (Koniec miejscowości)**: Opcja przekreślenia czerwoną linią diagonalną biegnącą z lewego dolnego rogu do prawego górnego rogu.
* **Wysokość ~3 bloków**: Konstrukcja stojąca na dwóch słupach ze stali ocynkowanej z realistycznymi ceownikami montażowymi i obejmami z tyłu tablicy.
* **Pełna konfiguracja MTR Brushem**:
  * Prawy klik pędzlem MTR otwiera okno konfiguracji: wpisanie nazwy miejscowości oraz przełącznik „Koniec miejscowości (Znak E-18a)”.
  * **Shift + prawy klik pędzlem MTR** natychmiast przełącza tryb tablicy między E-17a (początek) a E-18a (koniec miejscowości).

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
Zbudowany plik `.jar` (`mtr-polish-pack-1.0.0.jar`) pojawi się w katalogu `build/libs/`.