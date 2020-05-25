# The sources of opmac-tricks web site

Opmac-trick web site http://petr.olsak.net/opmac-tricks-e.html
have its sources in the files directory.

# How to use converter

`java -jar Converter.jar --input path/to/input/files --output path/where/to/generate --template path/to/templates --recents 5`

Options __--input__ can be shortened to __-i__, __--output__ to __-o__, __--template__ to __-t__ and __--recents__ to __-r__

argument --recents says how big the list with recent files should be, or zero if the list should not be present at all. Default value is 0


In template directory there should be files `template.html`, `template.tex` and `translations.txt`. All other files will get copied out to output folder (for example images for html page)

# Input file format

Each file contains several sections. First is section for metadata, in format `<KEY>:<VALUE>`. All other sections represent given article in language
Article section starts with four or more hyphens and contains meta key:value pairs. Supported keys in this context are `lang` denoting iso name of lang in which the article is written in and `title` which states the title of article in that language.
Keys:value pairs are delimited by hyphen, title must be the last key, all hyphens it title will not be treated as key separator.

Example of such valid section header line: `----lang:cs--title:Korekce střední výšky písma`

All text after this line is treated as content of that section, unless the line matches described format to denote new section (in new language)

## File level metadata

Each file must these several meta data attributes:

 *  `anchor-name` - name of anchor used in html. Arbitrary unique string
 *  `id` - id of this article, four digit number used so far, but arbitrary unique string will do
 *  `author` - name of author or authors (separated by comma `,`) which worked on this article. Each author can optinally have email address assosiated with him using `name <email>` notation.
 *  `date` - date of publishing this article, in ISO format (`YYYY-MM-DD`)


## Format of articles in sections

Article is an ordered set of paragraphs and code blocks. Paragraphs are separated by empty line, code blocks must start and end with three backticks on otherwise empty line.

Paragraph can contain other control sequences:

 *  inline code block - text which starts and ends with a single backtick. Single backtick can be escaped using double backtick
 *  image - standard markdown image: `![Ruzne urovne sekci](001-sections.jpg)` means "insert image, with name '001-sections.jpg' and set its description (title, alt) to 'Ruzne urovne sekci'"
 *  link - standard markdown link: `[Goto google](google.com)` means "create text link, which points to 'google.com' and text labels is 'Goto google'"
 *  non breaking space - is inserted automatically after single character words and manually using `~` character instead of space between words
 *  forced line break - use double backslash at the end of the line to force new line (`\\`)

## File structure and naming

Filenames of opmac-trick files have to start with three digits followed by a hyphen and then arbitrary string. For example `010-korekce-stredni-vysky-pisma.txt`
Files are automatically ordered using the number at the begining. Any files not following this naming pattern will be ignored.
Files are in folders, baseed on their category. Folders have to be named the same way - three digits, hyphen, optional text. Likewise, order of categories is given by value of number at the begining.
Each folder must contain one additional file `config.txt`. This file contains information about name of the category in different languages.
Format of this file is same as section header lines in standard files, just without the section body. Here is example by valid config file:

```
----lang:cs--name:Písmo

----lang:en--name:Fonts

```


## Example of valid article file:


anchor-name:svyska
id:0001
author:P. O.
date:2013-08-13

----lang:cs--title:Korekce střední výšky písma
Pro některá písma se může stát, že například strojopis `\tt` neladí se
základním písmem. Nastává to tehdy, když není stejná střední výška písma, třebaže
obě písma jsou zavedana ve stejné velikosti (například `at11pt`). \\
Korekci lze v OPmac udělat jednoduše využitím \thefontscale, který škáluje vzhledem
k~aktuální velikosti písma (nikoli vzhledem k~fixní designované velikosti).
Takže stačí třeba psát:

```
\def\tt{\tentt\thefontscale[1120]}
```

Uvedená definice zvětší \tt font 1,12 krát vzhledem k velikosti okolního
fontu. Přepínač \tt toto udělá kdekoli při jakékoli aktuální velikosti
okolního fontu. Uvedený poměr se hodí například pro kombinaci Bookman s
Courier.

----lang:en--title:The ex height correction
The visual incompatibility of font height can sometimes occur. For
example typewriter font `\tt` has different ex height than normal text font
even though both fonts are loaded at the same size. The correction can be
done simply by `\thefontscale` macro from OPmac. The scale factor is based on
the current size of the font (no on the fixed design size).
The following macro can be used, for example:

```
\def\tt{\tentt\thefontscale[1120]}
```

This definition scales the \tt font 1.12 times with respect to surrounded
font. The current font size can be arbitrary. The example above with the
ratio 1.12 is suitable for Bookman + Courier fonts.
