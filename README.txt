An NBT editor for files with the NBT format

NBT stands for NamedBinaryTag

Accepts program argument "demo" to automatically generate and open a demonstration file. (Deprecated)

What can it do safely:
-Edit files with NBT format
--Create/edit/delete any tag with type identifier 1 to 12
--Hex edit types 7 11 and 12
--Write/load raw file content as tags of type 7,11 or 12
-Encode and decode data in GZIP or ZLIB
-Create NBT files in GZIP encoding
-Undo-Redo operations, undo-able edit manager up to 256 edits, efficiently.

What can it do unsafely:
-Open a folder and multiple files in it at once (Glitches rarely)
-Load and display the MCRegion file format

What it does wrong:
-Write Region files (The output format can not be recognized by MC)

What it can't do:
-Create files with NBT formats of different encoding
-Write proper region file
