:


echo "Setting up the remotedemo test suite"

tmp=tmp$$

trap 's=$?; rm -f $tmp; exit $s' 0
trap 'exit $?' 1 2 3 13 15

test -r tetdist.orig || exit 1

sed "s%^\(TET_.*ROOT=\)tet-root%\1${TET_ROOT:?}%" tetdist.orig > tmp$$ && \
	mv $tmp tetdist.cfg

cp systems.orig systems

# for how many client systems do
rm -rf remclient? 
for i in 1 2 
do
	tar xf remclient.tar
	mv remclient remclient${i}
	echo "Set up client ${i}"
done
