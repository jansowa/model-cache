<h1>ANALIZA PROBLEMU</h1>

<h2>1. Co musi zapewniać model</h2>
<ul>
<li>przechowywać odnośniki do ostatnio używanych plików w chmurze wraz z informacjami o nich (takimi jak data utworzenia, data modyfikacji, nazwa itp.)</li>
<li>zapewniać podstawowe operacje bazodanowe</li>
<li>zapewniać wysoką wydajność</li>
<li>zapewniać łatwe modyfikowanie i utrzymywanie modelu</li>
<li>działać na wszystkich platformach (desktop, Android, iOS)</li>
<li>odpowiedni interfejs</li>
<li>implementacja interfejsu nie jest częścią zadania</li>
<li>umożliwiać dodawanie funkcjonalności do bazy przy możliwie najmniejszej ingerencji w już napisany kod</li>
<li>umożliwiać zmianę implementacji poszczególnych fragmentów (takich jak konkretne operacje na plikach, przechowywane informacje o nich) bez modyfikacji innych fragmentów kodu i łatwe podmienianie takich implementacji</li>
<li>usuwać najstarsze pliki modelu kiedy nie ma miejsca na nowe</li>
<li>umożliwiać pobieranie pliku z chmury i konwertowanie go do wersji zapisywanej na urządzeniu (np. na plik JSON z podstawowymi danymi)</li>
</ul>

<h2>2. Propozycje przechowywania danych</h2>
<h3>2.1 Podział ze względu na zastosowany język</h3>
<h4>2.1.1 Napisanie jednego klienta w języku Java kompilowanego na urządzenia mobilne za pomocą specjalistycznych narzędzi (takich jak Gluon z JavaFX)</h4>
<h4>2.1.2 Napisanie jednego klienta w kotlinie działającego na trzech platformach</h4>
Powyższe dwa rozwiązania mają pewne cechy wspólne.
Zalety:
<ul>
<li>jeden klient, dzięki temu piszemy zdecydowanie mniej kodu niż implementując trzy klienty, dużo łatwiej modyfikować takie rozwiązanie (zmieniając funkcjonalność dodajemy poprawki w jednym kodzie, nie w trzech)</li>
<li>nie musimy mieć specjalistycznej wiedzy dotyczącej programowania na wszystkie trzy platformy</li>
</ul>
Wady:
<ul>
<li>nie możemy korzystać z rozwiązań specjalistycznych dla danych platform (takich jak wysokowydajna baza ObjectBox na Androida)</li>
</ul>

Mamy też różnice pomiędzy 2.1.1 i 2.1.2.
JavaFX+Gluon:
<ul>
<li>musimy liczyć się ze spadkiem wydajności przy stosowaniu języka Java na iOS <a href="https://www.quora.com/If-Java-code-can-run-on-any-platform-why-are-iOS-developers-using-Objective-C-and-not-Java-to-run-on-the-iPhone">[1]</a></li>
<li>pisanie prostych widoków w JavaFX jest szybkie (i proste dzięki SceneBuilder), jednak JavaFX nie jest wydajnym narzędziem <a href="https://stackoverflow.com/questions/44136040/performance-of-javafx-gui-vs-swing">[2]</a></li>
<li>obecna wersja Gluona (stan na październik/listopad 2018) przynajmniej na niektórych komputerach nie działa na obecnej wersji IntelliJ Idea i Android SDK <a href="https://elenx.net/blank/Raicoone/issues/4#note_7253">[3]</a></li>
</ul>
<h4>2.1.3 Napisanie trzech niezależnych klientów dla wersji desktop+Android+iOS</h4>
Zalety:
<ul>
<li>piszemy w językach dedykowanych dla danych urządzeń, co powinno skutkować najwyższą wydajnością i największym wsparciem technicznym</li>
<li>możliwość korzystania z wysokowydajnych narzędzi przeznaczonych dla danych urządzeń - takich jak nierelacyjna baza danych ObjectBox na Androida</li>
</ul>
Wady:
<ul>
<li>developer musi potrafić programować na wszystkie 3 platformy jednocześnie (lub musimy wykorzystać 3 developerów)</li>
<li>wprowadzając dowolne modyfikacje implementacji musimy dokonywać ich w trzech kodach jednocześnie (kod trudny w utrzymaniu i modyfikacji)</li>
<li>znacznie większa ilość kodu do napisania (około trzykrotnie)</li>
</ul>
<h3>2.2 Podział ze względu na sposób przechowywania danych wewnątrz konkretnego języka</h3>
<h4>2.2.1 Struktury danych dostarczone w paczkach danego języka</h4>
Zalety:
<ul>
<li>najprostsza i najszybsza implementacja</li>
<li>prosta modyfikacja kodu</li>
<li>czytelny kod</li>
</ul>
Wady:
<ul>
<li>wysokie koszta operacji na danych</li>
</ul>
<h4>2.2.2 Pliki tymczasowe</h4>
Przechowujemy pliki w strukturze identycznej jak na chmurze. Pliki mają zawierać odnośniki do ich pełnych wersji w chmurze i podstawowe inforacje (np. data utworzenia).
Zalety:
<ul>
<li>zapisywanie i pobieranie plików jest mniej skomplikowane niż dodawanie ich do specjalnej struktury (jak bazy danych)</li>
<li>przenoszenie danych przy plikach tymczasowych jest prostym procesem</li>
</ul>
Wady:
<ul>
<li>niski poziom bezpieczeństwa - możliwość manualnego usunięcia plików (np. przez inną osobę) lub przez złośliwe oprogramowanie</li>
</ul>
<h4>2.2.3 Relacyjna baza danych</h4>
Tabele reprezentują foldery z chmury. Rekordy odpowiadają konkretnym plikom, kolumny zawierają kolejne informacje o nich (np. rozszerzenie, data utworzenia).

<h4>2.2.4 Nierelacyjna baza danych</h4>
Jeśli dane przechowujemy jako obiekty każdy z nich zawierałby pola reprezentujące podstawowe informacje (URL pliku w chmurze, rozszerzenie itp.). Obiekty reprezentujące foldery zawierałyby referencje do obiektów przedstawiających pliki znajdujące się wewnątrz folderu.
Wspólne zalety (baz relacyjnych i nierelacyjnych):
<ul>
<li>większe bezpieczeństwo niż pliki tymczasowe (np. brak możliwości przypadkowego przeniesienia danych z bazy jak przeniesienie plików/folderów w graficznym menedżerze plików)</li>
</ul>
<h2>3. Opis uproszczonego modelu testowego</h2>
<p>Zaimplementuję uproszczony model w celu porównania kilku rozwiązań. Model będzie podstawą do analizy konkretnych rozwiązań.</p>
<h2>4. Sprawy do rozstrzygnięcia przed rozpoczęciem pisania modelu testowego</h2>
<ul>
<li>wybór struktury danych do reprezentacji 2.2.1, także decyzja w jakim formacie mają być zapisywane pliki (przykładowo JSON, XML, serializowane obiekty)</li>
<li>wybór szybkiej relacyjnej bazy danych <a href="https://stackoverflow.com/questions/439958/highest-performance-database-in-java">[4]</a></li>
<li>wybór szybkiej nierelacyjnej bazy danych</li>
<li>określenie, które informacje poza adresem URL do pliku w chmurze ma przechowywać model</li>
<li>dobór odpowiedniego narzędzia do testu rozwiązania 2.1.1 (kod Java kompilowany na urządzenia mobilne)</li>
<li>określenie jak dokładnie rekordy mają być zapisywane w relacyjnych bazach danych (czy każdy folder ma posiadać własną tabelę? czy pliki, foldery potrzebują specjalnej kolumny na element nadrzędny (+podrzędny dla samych folderów)? czy foldery mają mieć takie same właściwości jak pliki z NULL-em jako niektóre właściwości? ew. zastosowanie specjalnego typu rozszerzenia jako trick do zapisywania folderów (przykładowo rozszerzenie .dir)</li>
<li>określenie jak dokładnie mają wyglądać zapisy w nierelacyjnej bazie danych - problemy analogiczne do powyższego podpunktu (np. czy jeśli dane będą zapisywane jako obiekty folder i plik mają być innymi obiektami? jak przechowywać informacje o relacji plików/folderów względem siebie (przykładowo elementy nadrzędne/podrzędne jako pola)? czy stosować specjalne rozszerzenie .dir dla folderów?</li>
</ul>

<h2>5. Do zbadania przez model testowy</h2>
<ul>
<li>czytelność kodu konkretnych rozwiązań</li>
<li>wydajność konkretnych rozwiązań</li>
<li>ile zapisów potrafi przechowywać model przy określonej pojemności</li>
<li>możliwość prostej modyfikacji kodu</li>
</ul>

<h2>6. Dodatkowe uwagi</h2>
<p>Pracując nad badaniem mocno opieram się na niemalże identycznym badaniu dotyczącym wyłącznie rozwiązania na system Android <a href="https://elenx.net/blank/tabula/wikis/2_Research/Najwydajniejszy-system-przetrzymywania-danych-dla-aplikacji-Raicoone">[5]</a></p>

<p>Moim celem jest początkowo napisanie sensownego interfejsu modelu cache, następnie wykonanie modelu testowego.</p>

<p>Finalna implementacja będzie zależna od technologii zastosowanych w pisaniu kompletnych klientów Raicoone. Ze względu na spore podobieństwo w implementacji Kotlin i rozwiązania 2.1.1 (Java + narzędzie umożliwiające kompilację kodu na urządzenia mobilne) skupię się wyłącznie na porównaniu 2.1.1 (Java+dodatkowe narzędzie) i 2.1.2 (3 implementacje na 3 platformy). Przerobienie jednego rozwiązanie na drugie powinno być proste przy podstawowej znajomości składni Kotlin, dodatkowo ewentualnie popularnych zewnętrznych paczek.</p>

<p>W modelu testowym najpierw zbadamy które rozwiązanie jest najlepsze w języku Java i sprawdzimy jego działanie za pomocą jednego z narzędzi do programowania na kilka platform jednocześnie. Później porównamy wydajność do ObjectBoxa (według badania <a href="https://elenx.net/blank/tabula/wikis/2_Research/Najwydajniejszy-system-przetrzymywania-danych-dla-aplikacji-Raicoone">[5]</a> najszybsze rozwiązanie w systemie Android). Ze względu na obecne problemy z projektem Gluon (który był już badany przez ElenX) skorzystam z innego podobnego rozwiązania zalecanego przez programistów w sieci. Informacje dotyczące tego typu narzędzi: <a href="https://medium.com/@mateusz_bartos/write-ios-apps-in-java-along-with-android-900d6013f83f">[6]</a>,
<a href="https://stackoverflow.com/questions/2050943/how-can-one-develop-iphone-apps-in-java">[7]</a></p>

<h2>7. Materiały zewnętrzne:</h2>
<ol>
<li>https://www.quora.com/If-Java-code-can-run-on-any-platform-why-are-iOS-developers-using-Objective-C-and-not-Java-to-run-on-the-iPhone</li>
<li>https://stackoverflow.com/questions/44136040/performance-of-javafx-gui-vs-swing</li>
<li>https://elenx.net/blank/Raicoone/issues/4#note_7253</li>
<li>https://stackoverflow.com/questions/439958/highest-performance-database-in-java</li>
<li>https://elenx.net/blank/tabula/wikis/2_Research/Najwydajniejszy-system-przetrzymywania-danych-dla-aplikacji-Raicoone</li>
<li>https://medium.com/@mateusz_bartos/write-ios-apps-in-java-along-with-android-900d6013f83f</li>
<li>https://stackoverflow.com/questions/2050943/how-can-one-develop-iphone-apps-in-java</li>
<ol>
