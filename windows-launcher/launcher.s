    .section .text,"ax"
    .globl _start
_start:
    pushl $32768
    pushl $module_path
    pushl $0
    call *iat_GetModuleFileNameA
    testl %eax, %eax
    jz launch_error

    movl $module_path, %esi
    addl %eax, %esi
find_directory:
    cmpl $module_path, %esi
    jbe launch_error
    decl %esi
    cmpb $'\\', (%esi)
    jne find_directory
    incl %esi
    movb $0, (%esi)

    pushl $module_path
    pushl $java_path
    call *iat_lstrcpyA
    pushl $java_suffix
    pushl $java_path
    call *iat_lstrcatA

    pushl $module_path
    pushl $jar_path
    call *iat_lstrcpyA
    pushl $jar_name
    pushl $jar_path
    call *iat_lstrcatA

    pushl $java_path
    call *iat_GetFileAttributesA
    cmpl $-1, %eax
    je launch_error
    pushl $jar_path
    call *iat_GetFileAttributesA
    cmpl $-1, %eax
    je launch_error

    pushl $parameter_prefix
    pushl $parameters
    call *iat_lstrcpyA
    pushl $module_path
    pushl $parameters
    call *iat_lstrcatA
    pushl $parameter_suffix
    pushl $parameters
    call *iat_lstrcatA

    pushl $1
    pushl $module_path
    pushl $parameters
    pushl $java_path
    pushl $open_verb
    pushl $0
    call *iat_ShellExecuteA
    cmpl $32, %eax
    jbe launch_error

    pushl $0
    call *iat_ExitProcess

launch_error:
    pushl $0x10
    pushl $caption
    pushl $error_text
    pushl $0
    call *iat_MessageBoxA
    pushl $1
    call *iat_ExitProcess

    .section .rdata,"a"
open_verb:
    .asciz "open"
java_suffix:
    .asciz "jre\\bin\\javaw.exe"
jar_name:
    .asciz "NOS-Pipe.jar"
parameter_prefix:
    .asciz "-cp \""
parameter_suffix:
    .asciz "NOS-Pipe.jar\" notpipe.gui.NOSPipeGui"
caption:
    .asciz "NOS-Pipe"
error_text:
    .asciz "NOS-Pipe could not start. Keep NOS-Pipe.exe beside NOS-Pipe.jar and the jre folder."

    .section .bss,"aw",@nobits
    .align 4
module_path:
    .space 32768
java_path:
    .space 32768
jar_path:
    .space 32768
parameters:
    .space 65536

    .section .idata,"aw"
    .align 4
import_descriptors:
    .long ilt_kernel32 - 0x400000, 0, 0, kernel32_name - 0x400000, iat_kernel32 - 0x400000
    .long ilt_shell32 - 0x400000, 0, 0, shell32_name - 0x400000, iat_shell32 - 0x400000
    .long ilt_user32 - 0x400000, 0, 0, user32_name - 0x400000, iat_user32 - 0x400000
    .long 0, 0, 0, 0, 0

ilt_kernel32:
    .long hn_GetModuleFileNameA - 0x400000
    .long hn_GetFileAttributesA - 0x400000
    .long hn_lstrcpyA - 0x400000
    .long hn_lstrcatA - 0x400000
    .long hn_ExitProcess - 0x400000
    .long 0
ilt_shell32:
    .long hn_ShellExecuteA - 0x400000
    .long 0
ilt_user32:
    .long hn_MessageBoxA - 0x400000
    .long 0

iat_kernel32:
iat_GetModuleFileNameA: .long hn_GetModuleFileNameA - 0x400000
iat_GetFileAttributesA: .long hn_GetFileAttributesA - 0x400000
iat_lstrcpyA:           .long hn_lstrcpyA - 0x400000
iat_lstrcatA:           .long hn_lstrcatA - 0x400000
iat_ExitProcess:        .long hn_ExitProcess - 0x400000
                        .long 0
iat_shell32:
iat_ShellExecuteA:      .long hn_ShellExecuteA - 0x400000
                        .long 0
iat_user32:
iat_MessageBoxA:        .long hn_MessageBoxA - 0x400000
                        .long 0

kernel32_name: .asciz "KERNEL32.dll"
shell32_name:  .asciz "SHELL32.dll"
user32_name:   .asciz "USER32.dll"
    .align 2
hn_GetModuleFileNameA: .short 0; .asciz "GetModuleFileNameA"; .align 2
hn_GetFileAttributesA: .short 0; .asciz "GetFileAttributesA"; .align 2
hn_lstrcpyA:           .short 0; .asciz "lstrcpyA"; .align 2
hn_lstrcatA:           .short 0; .asciz "lstrcatA"; .align 2
hn_ExitProcess:        .short 0; .asciz "ExitProcess"; .align 2
hn_ShellExecuteA:      .short 0; .asciz "ShellExecuteA"; .align 2
hn_MessageBoxA:        .short 0; .asciz "MessageBoxA"; .align 2
