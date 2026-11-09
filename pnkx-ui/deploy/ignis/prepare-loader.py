#!/usr/bin/env python3
"""Prepend our compatibility shim to an existing Ignis loader (no Obsidian edits)."""
import argparse
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('source', type=Path, help='shim-loader.js copied from the running container')
parser.add_argument('output', type=Path, help='separate output file for a read-only bind mount')
args = parser.parse_args()
if args.source.resolve() == args.output.resolve():
    parser.error('source and output must differ; preserve the original loader')
shim = Path(__file__).with_name('embed-compat.js').read_bytes()
loader = args.source.read_bytes()
if b'installEmbeddedRendererBoundary' in loader:
    parser.error('loader already contains the compatibility shim')
if b'__ignis' not in loader:
    parser.error('source does not look like an Ignis loader')
args.output.write_bytes(shim + b'\n;\n' + loader)
print(f'Prepared {args.output} (original unchanged)')
