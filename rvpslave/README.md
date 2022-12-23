rvpslave/: files for creating an `rvpslave` user on rvwork-1, who
    Jenkins can boss around

	authorized_keys: the public part of Jenkins' SSH key pair
	    on rv-server-1
	mkrvpslave: create the user and copy essential files into
	    its home directory
	rvpslave-key,
	rvpslave-key.pub: public/private authorized key-pair [UNUSED]
	uprvpslave: update the `rvpslave` home directory using the content
	    of this directory
