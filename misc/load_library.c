#define _GNU_SOURCE
#include <assert.h>
#include <dlfcn.h>
#include <limits.h>
#include <link.h>
#include <linux/limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

static int
callback(struct dl_phdr_info *info, size_t size, void *data)
{
	char *type;
	int p_type, j;

	printf("Name: \"%s\" (%d segments)\n", info->dlpi_name,
		info->dlpi_phnum);

	for (j = 0; j < info->dlpi_phnum; j++) {
		p_type = info->dlpi_phdr[j].p_type;
		type =  (p_type == PT_LOAD) ? "PT_LOAD" :
				(p_type == PT_DYNAMIC) ? "PT_DYNAMIC" :
				(p_type == PT_INTERP) ? "PT_INTERP" :
				(p_type == PT_NOTE) ? "PT_NOTE" :
				(p_type == PT_INTERP) ? "PT_INTERP" :
				(p_type == PT_PHDR) ? "PT_PHDR" :
				(p_type == PT_TLS) ? "PT_TLS" :
				(p_type == PT_GNU_EH_FRAME) ? "PT_GNU_EH_FRAME" :
				(p_type == PT_GNU_STACK) ? "PT_GNU_STACK" :
				(p_type == PT_GNU_RELRO) ? "PT_GNU_RELRO" : NULL;

		printf("	%2d: [%14p; memsz:%7lx] flags: 0x%x; ", j,
				(void *) (info->dlpi_addr + info->dlpi_phdr[j].p_vaddr),
				info->dlpi_phdr[j].p_memsz,
				info->dlpi_phdr[j].p_flags);
		if (type != NULL)
			printf("%s\n", type);
		else
			printf("[other (0x%x)]\n", p_type);
		printf("	p_offset:%ld p_vaddr:%ld p_paddr:%ld p_filesz:%ld p_memsz:%ld p_align:%ld\n",
				info->dlpi_phdr[j].p_offset,
				info->dlpi_phdr[j].p_vaddr,
				info->dlpi_phdr[j].p_paddr,
				info->dlpi_phdr[j].p_filesz,
				info->dlpi_phdr[j].p_memsz,
				info->dlpi_phdr[j].p_align);
	}

	return 0;
}

void print_function(char* name, void* observed_address) {
	void *handle;
	char *error;

	handle = dlopen("libc.so.6", RTLD_LAZY);
	if (!handle) {
		printf("%10s: %14p\n", name, observed_address);
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}

	dlerror();	/* Clear any existing error */
	printf("%10s: %14p / %14p\n", name, observed_address, dlsym(handle, name));
	error = dlerror();
	if (error != NULL) {
		fprintf(stderr, "%s\n", error);
		exit(EXIT_FAILURE);
	}

	dlclose(handle);
}

void print_link_map_entry(struct link_map* map) {
	printf("  link_map: %14p\n", map);
	printf("	l_addr: %lx\n", map->l_addr);
	printf("	l_ld: %14p\n", map->l_ld);
	printf("	  d_tag: %lx\n", map->l_ld->d_tag);
	printf("	  d_val: %ld\n", map->l_ld->d_un.d_val);
	printf("	  d_ptr: %14p\n", (void*)map->l_ld->d_un.d_ptr);
}

void print_library(char* name) {
	printf("Loading library: %s\n", name);
	void *handle;
	handle = dlopen("libc.so.6", RTLD_LAZY);
	if (!handle) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	printf("  Handle: %14p\n", handle);
	
	Lmid_t lmidt;
	if (dlinfo(handle, RTLD_DI_LMID, &lmidt)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	printf("  Namespace: 0x%016lx\n", lmidt);
	
	struct link_map* map;
	if (dlinfo(handle, RTLD_DI_LINKMAP, &map)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	struct link_map* current = map;
	while (current != NULL) {
		print_link_map_entry(current);
		current = current->l_next;
	}
	current = map->l_prev;
	while (current != NULL) {
		print_link_map_entry(current);
		current = current->l_prev;
	}

	char path[PATH_MAX + 1];
	if (dlinfo(handle, RTLD_DI_ORIGIN, path)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	printf("  Origin pathname: %s\n", path);

	Dl_serinfo serinfo_for_size;
	if (dlinfo(handle, RTLD_DI_SERINFOSIZE, &serinfo_for_size)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	
	Dl_serinfo *serinfo = malloc(serinfo_for_size.dls_size);
	if (dlinfo(handle, RTLD_DI_SERINFOSIZE, serinfo)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	assert(serinfo->dls_size == serinfo_for_size.dls_size);
	if (dlinfo(handle, RTLD_DI_SERINFO, serinfo)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	printf("  Serinfo size:\n");
	printf("	size: %lu\n", serinfo_for_size.dls_size);
	printf("	size: %lu\n", serinfo->dls_size);
	printf("	count: %u\n", serinfo->dls_cnt);
	assert(serinfo->dls_size == serinfo_for_size.dls_size);
	for (int i = 0; i < serinfo->dls_cnt; i++) {
		printf("	path[%d]\n", i);
		printf("	  path: %s\n", serinfo->dls_serpath[i].dls_name);
		printf("	  flags: %u\n", serinfo->dls_serpath[i].dls_flags);
	}
	free(serinfo);

	size_t size;
	if (dlinfo(handle, RTLD_DI_TLS_MODID, &size)) {
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}
	printf("  Module id: %lu\n", size);

	dlclose(handle);
}

int main(int argc, char *argv[])
{
	dl_iterate_phdr(callback, NULL);

	printf("main: %14p\n", main);
	printf("callback: %14p\n", callback);
	printf("printf: %14p\n", printf);
	printf("fopen: %14p\n", fopen);
	printf("stderr: %14p\n", &stderr);
	printf("strlen: %14p\n", &strlen);
	print_function("strlen", strlen);

	print_library("libc.so.6");

	
	exit(EXIT_SUCCESS);
}
