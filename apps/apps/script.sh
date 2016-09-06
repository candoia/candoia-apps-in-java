
for f in setting1/bugFileMapper/*; do
  added=`diff -u -s  $f setting2/bugFileMapper/ | grep ^+ | wc -l`
  removed=`diff -u -s  $f setting2/bugFileMapper/ | grep ^- | wc -l`
  echo $f  $added $removed 
done

