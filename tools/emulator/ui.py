#!/usr/bin/env python3
"""Leitura de dumps do uiautomator (window_dump.xml) para os roteiros de uso.

Uso:
  ui.py find  <dump.xml> <texto> [--contains] [--index N] [--class EditText]
        -> imprime "x y" (centro dos bounds) do N-ésimo nó cujo text ou content-desc casa
  ui.py texts <dump.xml>   -> lista textos/descrições visíveis (um por linha)
  ui.py edits <dump.xml>   -> lista campos editáveis: "idx x y texto"
Saída 1 quando não encontra.
"""
import re
import sys
import xml.etree.ElementTree as ET


def nodes(path):
    root = ET.parse(path).getroot()
    for n in root.iter("node"):
        yield n


def center(n):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def label(n):
    return (n.get("text") or "").strip() or (n.get("content-desc") or "").strip()


def main(argv):
    sys.stdout.reconfigure(newline="\n", encoding="utf-8")  # sem \r no Windows
    cmd, path = argv[1], argv[2]
    if cmd == "str":  # ui.py str <strings.xml> <nome> [args...]  -> texto do recurso formatado
        root = ET.parse(path).getroot()
        for s in root.iter("string"):
            if s.get("name") == argv[3]:
                t = "".join(s.itertext())
                t = t.replace("\\u00A0", "\u00a0").replace("\\'", "'").replace('\\"', '"')
                t = t.replace("%%", "%")
                for i, v in enumerate(argv[4:], 1):
                    t = re.sub(r"%%%d\$[sd]" % i, v, t)
                print(t)
                return 0
        return 1
    if cmd == "texts":
        for n in nodes(path):
            t = label(n)
            if t:
                print(t)
        return 0
    if cmd == "edits":
        i = 0
        for n in nodes(path):
            if "EditText" in (n.get("class") or ""):
                x, y = center(n)
                print(i, x, y, label(n))
                i += 1
        return 0 if i else 1
    if cmd == "find":
        text = argv[3]
        contains = "--contains" in argv
        index = int(argv[argv.index("--index") + 1]) if "--index" in argv else 0
        cls = argv[argv.index("--class") + 1] if "--class" in argv else None
        hits = []
        for n in nodes(path):
            if cls and cls not in (n.get("class") or ""):
                continue
            for t in ((n.get("text") or "").strip(), (n.get("content-desc") or "").strip(),
                      "id:" + (n.get("resource-id") or "")):
                if t and t != "id:" and (text in t if contains else t == text):
                    c = center(n)
                    if c and c != (0, 0):
                        hits.append(c)
                    break
        if len(hits) > index:
            print(*hits[index])
            return 0
        return 1
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
