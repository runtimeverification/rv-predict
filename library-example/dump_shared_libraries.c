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

void dump_libraries() {
    dl_iterate_phdr(callback, NULL);
}