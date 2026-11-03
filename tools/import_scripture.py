"""Rebuild from the pinned official WEB archive, or verify the checked-in offline catalog.

Python 3 standard library only. No network calls; obtain the archive separately from SOURCE_URL.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE_URL = "https://ebible.org/Scriptures/eng-web_vpl.zip"
ARCHIVE_SHA256 = "fd2ddfed425dabe379b03d1bf9d7244e676a3ffb1fb74eed9dd1e9e1e900a836"
REQUIRED_THEMES = set("temptation sexual-temptation self-control strength perseverance anxiety fear loneliness sadness hopelessness guilt forgiveness repentance love identity peace renewal discipline wisdom courage endurance prayer trust hope gratitude anger patience bondage freedom".split())
CODES = "GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH EST JOB PSA PRO ECC SNG ISA JER LAM EZK DAN HOS JOL AMO OBA JON MIC NAM HAB ZEP HAG ZEC MAL MAT MRK LUK JHN ACT ROM 1CO 2CO GAL EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB JAS 1PE 2PE 1JN 2JN 3JN JUD REV".split()
VPL_CODES = dict(zip(CODES, "GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH EST JOB PSA PRO ECC SOL ISA JER LAM EZE DAN HOS JOE AMO OBA JON MIC NAH HAB ZEP HAG ZEC MAL MAT MAR LUK JOH ACT ROM 1CO 2CO GAL EPH PHI COL 1TH 2TH 1TI 2TI TIT PHM HEB JAM 1PE 2PE 1JO 2JO 3JO JUD REV".split()))
SOURCE = ROOT / "data/scripture_source"
CATALOG = ROOT / "data/scripture_catalog.json"
ASSET = ROOT / "app/src/main/assets/scripture_catalog.json"
BOUNDS_ASSET = ROOT / "app/src/main/assets/scripture_verse_counts.json"
CURATION = ROOT / "data/scripture_curation.tsv"
BOOKS = json.loads((ROOT / "data/bible_books.json").read_text(encoding="utf-8"))["books"]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def source_verses(xml):
    result = {}
    for verse in ET.fromstring(xml):
        if verse.attrib["b"] not in CODES:
            continue
        key = (verse.attrib["b"], int(verse.attrib["c"]), int(verse.attrib["v"]))
        assert key not in result, f"Duplicate source verse {key}"
        # The publisher's VPL removes presentation markup. Preserve its words and punctuation;
        # trim only boundary whitespace, then join adjacent complete verses with one space.
        result[key] = "".join(verse.itertext()).strip()
    return result


def curated(source, bounds):
    entries, used, references, texts = [], set(), set(), set()
    for line in CURATION.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        book_id, themes, passages = line.split("\t")
        book = BOOKS[int(book_id) - 1]
        code = CODES[book["id"] - 1]
        tags = themes.split(",")
        assert tags and len(tags) == len(set(tags)) and all(re.fullmatch("[a-z]+(?:-[a-z]+)*", t) for t in tags)
        for passage in passages.split():
            match = re.fullmatch(r"(\d+):(\d+)(?:-(\d+))?", passage)
            assert match, passage
            chapter, start = int(match[1]), int(match[2])
            end = int(match[3] or start)
            assert 1 <= chapter <= book["chapters"]
            assert 1 <= start <= end <= bounds[code][chapter - 1]
            keys = [(code, chapter, v) for v in range(start, end + 1)]
            assert not used.intersection(keys), f"Overlapping passage {book['name']} {passage}"
            used.update(keys)
            text = " ".join(source[key] for key in keys)
            assert text.strip() == text and text and "<" not in text
            reference = f"{book['name']} {chapter}:{start}" + (f"-{end}" if end != start else "")
            assert reference not in references and text not in texts
            references.add(reference)
            texts.add(text)
            entries.append(dict(id=f"web-{code}-{chapter:03}-{start:03}-{end:03}",
                                bookId=book["id"], book=book["name"], chapter=chapter,
                                verseStart=start, verseEnd=end, reference=reference, text=text,
                                testament=book["testament"], tags=tags))
    assert len(entries) == 500, f"Expected exactly 500 passages, got {len(entries)}"
    assert {e["bookId"] for e in entries} == set(range(1, 67))
    assert all(200 <= sum(e["testament"] == t for e in entries) <= 300 for t in ("OT", "NT"))
    assert len({e["id"] for e in entries}) == 500
    theme_counts = Counter(t for e in entries for t in e["tags"])
    assert all(theme_counts[theme] >= 3 for theme in REQUIRED_THEMES), theme_counts
    return entries, used


def rebuild(archive):
    archive_bytes = archive.read_bytes()
    assert sha(archive_bytes) == ARCHIVE_SHA256, "Source archive differs from the pinned edition; review before updating."
    with zipfile.ZipFile(archive) as zipped:
        xml = zipped.read("eng-web_vpl.xml")
        vpl = zipped.read("eng-web_vpl.txt")
    source = source_verses(xml)
    plain = {}
    for line in vpl.decode("utf-8-sig").splitlines():
        if line:
            if line.split(" ", 1)[0] not in VPL_CODES.values():
                continue
            match = re.fullmatch(r"(\S+) (\d+):(\d+) (.*)", line)
            assert match, line
            plain[(match[1], int(match[2]), int(match[3]))] = match[4].strip()
    # Independently formatted official members must agree for every verse of the selected canon.
    for (code, chapter, verse), text in source.items():
        if code in CODES:
            assert plain[(VPL_CODES[code], chapter, verse)] == text, (code, chapter, verse)
    bounds = {}
    for code, book in zip(CODES, BOOKS):
        chapters = []
        for chapter in range(1, book["chapters"] + 1):
            numbers = {v for b, c, v in source if b == code and c == chapter}
            assert numbers and numbers == set(range(1, max(numbers) + 1)), (code, chapter)
            chapters.append(max(numbers))
        bounds[code] = chapters
    entries, used = curated(source, bounds)
    # Preserve literal publisher XML lines as independent reference evidence, not JSON-derived text.
    selected_lines = []
    for line in xml.decode("utf-8-sig").splitlines():
        if line.startswith("<v "):
            verse = ET.fromstring(line)
            if verse.attrib["b"] not in CODES:
                continue
            key = (verse.attrib["b"], int(verse.attrib["c"]), int(verse.attrib["v"]))
            if key in used:
                selected_lines.append(line)
    excerpt = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<verseFile>\n" + "\n".join(selected_lines) + "\n</verseFile>\n").encode("utf-8")
    assert set(source_verses(excerpt)) == used
    SOURCE.mkdir(parents=True, exist_ok=True)
    (SOURCE / "eng-web-selected.xml").write_bytes(excerpt)
    (SOURCE / "chapter_verse_counts.json").write_bytes(json_bytes(bounds))
    BOUNDS_ASSET.write_bytes(json_bytes({str(b["id"]): bounds[code] for code, b in zip(CODES, BOOKS)}))
    catalog = json_bytes(entries)
    CATALOG.write_bytes(catalog)
    ASSET.write_bytes(catalog)
    provenance = dict(translation="World English Bible Classic (WEB), 2020 stable text edition",
                      canon="Protestant 66-book subset of the publisher's ecumenical source",
                      sourceUrl=SOURCE_URL, editionUrl="https://ebible.org/find/details.php?id=eng-web",
                      licenseUrl="https://ebible.org/eng-web/copyright.htm", license="Public domain",
                      retrieved="2026-11-02", archiveSha256=ARCHIVE_SHA256,
                      members={"eng-web_vpl.xml": sha(xml), "eng-web_vpl.txt": sha(vpl)},
                      passageCount=500, verseCount=len(used), bookCount=66,
                      testamentCounts={t: sum(e["testament"] == t for e in entries) for t in ("OT", "NT")},
                      themeCounts=dict(sorted(Counter(t for e in entries for t in e["tags"]).items())),
                      files={"data/scripture_catalog.json": sha(catalog),
                             "data/scripture_curation.tsv": sha(CURATION.read_bytes()),
                             "data/scripture_source/eng-web-selected.xml": sha(excerpt),
                             "app/src/main/assets/scripture_verse_counts.json": sha(BOUNDS_ASSET.read_bytes()),
                             "data/scripture_source/chapter_verse_counts.json": sha(json_bytes(bounds))})
    (SOURCE / "provenance.json").write_bytes(json_bytes(provenance))


def verify():
    provenance = json.loads((SOURCE / "provenance.json").read_text(encoding="utf-8"))
    assert provenance["archiveSha256"] == ARCHIVE_SHA256
    for name, expected in provenance["files"].items():
        assert sha((ROOT / name).read_bytes()) == expected, f"Checksum mismatch: {name}"
    source = source_verses((SOURCE / "eng-web-selected.xml").read_bytes())
    bounds = json.loads((SOURCE / "chapter_verse_counts.json").read_text(encoding="utf-8"))
    assert sum(map(len, bounds.values())) == 1189
    assert json.loads(BOUNDS_ASSET.read_text(encoding="utf-8")) == {str(b["id"]): bounds[code] for code, b in zip(CODES, BOOKS)}
    entries, used = curated(source, bounds)
    assert dict(Counter(t for e in entries for t in e["tags"])) == provenance["themeCounts"]
    assert set(source) == used and len(source) == provenance["verseCount"]
    assert CATALOG.read_bytes() == ASSET.read_bytes() == json_bytes(entries), "Stale runtime/reference catalog"
    print(f"Verified {len(entries)} passages / {len(source)} exact source verses / 66 books; "
          + str(provenance["testamentCounts"]) + "; offline, checksums and XML/VPL provenance retained.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-archive", type=Path, help="Rebuild from the exact pinned official ZIP, then verify")
    arguments = parser.parse_args()
    if arguments.source_archive:
        rebuild(arguments.source_archive)
    verify()
