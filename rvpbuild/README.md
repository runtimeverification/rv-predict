rvpbuild/: files for creating an `rvpbuild` user on rvwork-1, who
    Jenkins can boss around

	authorized_keys: mapping from keys to srctar-to-* commands
	mkrvpbuild: create the user and copy essential files into
	    its home directory
	rvpbuild-installer-key,
	rvpbuild-installer-key.pub: public/private keypair for
	    srctar-to-installer
	rvpbuild-key,
	rvpbuild-key.pub: public/private keypair for srctar-to-deb
	srctar-to-deb: read RV-Predict/C sources on stdin, emit a
	    tar file consisting of stdout, stderr, and a .deb on
	    stdout.
	srctar-to-installer: read RV-Predict/C sources on stdin, emit a
	    tar file consisting of stdout, stderr, and an installer .jar on
	    stdout.
	uprvpbuild: update the `rvpbuild` home directory using the content
	    of this directory
