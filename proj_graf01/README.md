# Edytor 2D — linia, prostokąt, okrąg

## Funkcjonalności 
- **Rysowanie 3 prymitywów:** linia, prostokąt, okrąg
- **Dwa tryby definiowania:**
  - **Pola tekstowe** → wprowadź parametry i „Zastosuj/Dodaj”
  - **Mysz** → kliknięcia punktów charakterystycznych + przeciąganie
- **Edycja:**
  - **Zaznaczanie** obiektu
  - **Przesuwanie** (drag)
  - **Zmiana kształtu/rozmiaru** przez **uchwyty**
  - **Modyfikacja z pól tekstowych** (po zaznaczeniu)
- **Serializacja/Deserializacja:** zapis i odczyt sceny do/z **JSON**
- **Skróty:** Delete/Backspace (usuń), Esc (anuluj)


## Wykorzystane (technologie i wzorce)
- **Język/stack:** Java 21, **JavaFX (Canvas)**, Maven, **Gson (JSON)**
- **Wzorce:** 
  - **MVC** – podział na model / widok / kontroler
  - **Strategy** – narzędzia rysujące jako strategie (Line/Rect/Circle)
  - **Command** – operacje edycyjne z możliwością rozbudowy o undo/redo
  - **DTO** – zapis/odczyt kształtów do JSON
- **Obsługa wejścia:** mysz (klik/drag), klawiatura (Delete/Esc)
- **Walidacja danych:** podstawowa (promień > 0, szer./wys. > 0, strokeWidth ≥ 0, kolory HEX)
