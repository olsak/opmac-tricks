#/bin/bash

# rm -rf gen # Clean folder

java -jar Converter.jar --input files --template template --output gen --recents 5

cd gen

sed -i 's/opmac-tricks-en.html/opmac-tricks-e.html/g' opmac-tricks-cs.html
sed -i 's/opmac-tricks-cs.html/opmac-tricks.html/g' opmac-tricks-en.html
sed -i 's/opmac.html/opmac-e.html/g' opmac-tricks-en.html

mv opmac-tricks-cs.html opmac-tricks.html
mv opmac-tricks-en.html opmac-tricks-e.html

pdfcsplain opmac-tricks-cs.tex && pdfcsplain opmac-tricks-cs.tex
pdfcsplain opmac-tricks-en.tex && pdfcsplain opmac-tricks-en.tex
rm *.{log,ref}

# scp ... # Upload to server

