"""Dump mod ids/names and lang keys from a modpack mods folder.

Usage:
  python dump_pack_info.py <mods_dir> <out_dir>
"""
import io
import json
import os
import re
import sys
import zipfile


def read_text(zf, name):
    try:
        return zf.read(name).decode("utf-8", "replace")
    except KeyError:
        return None


def mods_toml_info(zf):
    raw = read_text(zf, "META-INF/mods.toml")
    if not raw:
        return None
    modid = re.search(r'modId\s*=\s*"([^"]+)"', raw)
    name = re.search(r'displayName\s*=\s*"([^"]+)"', raw)
    version = re.search(r'version\s*=\s*"([^"]+)"', raw)
    desc = re.search(r'description\s*=\s*\'\'\'(.*?)\'\'\'', raw, re.S)
    if not desc:
        desc = re.search(r'description\s*=\s*"(.*?)"', raw, re.S)
    return {
        "modid": modid.group(1) if modid else "",
        "name": name.group(1) if name else "",
        "version": version.group(1) if version else "",
        "desc": (desc.group(1).strip().replace("\n", " ")[:220] if desc else ""),
    }


def main():
    mods_dir, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    rows = []
    for fn in sorted(os.listdir(mods_dir)):
        if not fn.endswith(".jar"):
            continue
        path = os.path.join(mods_dir, fn)
        try:
            with zipfile.ZipFile(path) as zf:
                info = mods_toml_info(zf)
                if not info:
                    continue
                rows.append(
                    (
                        fn,
                        info["modid"] or "",
                        info["name"] or "",
                        info["version"] or "",
                        info["desc"] or "",
                    )
                )
                modid = info["modid"]
                if not modid:
                    continue
                for lang in ("en_us", "ru_ru"):
                    lang_name = f"assets/{modid}/lang/{lang}.json"
                    text = read_text(zf, lang_name)
                    if not text:
                        continue
                    try:
                        data = json.loads(text)
                    except Exception:
                        continue
                    with open(
                        os.path.join(out_dir, f"{modid}.{lang}.txt"),
                        "w",
                        encoding="utf-8",
                    ) as fh:
                        for k in sorted(data):
                            v = str(data[k]).replace("\n", "\\n")
                            fh.write(f"{k} = {v}\n")
        except Exception as exc:  # noqa: BLE001
            print(f"[warn] {fn}: {exc}", file=sys.stderr)
    with open(os.path.join(out_dir, "_mods.tsv"), "w", encoding="utf-8") as fh:
        for r in rows:
            fh.write("\t".join(r) + "\n")
    print(f"mods={len(rows)}")


if __name__ == "__main__":
    main()
