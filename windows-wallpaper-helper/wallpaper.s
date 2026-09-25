    .section .text,"ax"
    .globl _start
_start:
    call detect_silent

    # Locate the transparent PNG beside the installed helper.
    pushl $16384
    pushl $module_path
    pushl $0
    call *iat_GetModuleFileNameW
    testl %eax, %eax
    jz fatal_error
    leal module_path(,%eax,2), %esi
find_directory:
    cmpl $module_path, %esi
    jbe fatal_error
    subl $2, %esi
    cmpw $0x5c, (%esi)
    jne find_directory
    addl $2, %esi
    movw $0, (%esi)
    pushl $png_suffix
    pushl $module_path
    call *iat_lstrcatW

    # Build %APPDATA%\NOS-Pipe\NOS-Pipe-wallpaper.bmp.
    pushl $16384
    pushl $output_path
    pushl $appdata_name
    call *iat_GetEnvironmentVariableW
    testl %eax, %eax
    jz fatal_error
    pushl $folder_suffix
    pushl $output_path
    call *iat_lstrcatW
    pushl $0
    pushl $output_path
    call *iat_CreateDirectoryW
    pushl $file_suffix
    pushl $output_path
    call *iat_lstrcatW

    # Current primary-screen size.
    pushl $0
    call *iat_GetSystemMetrics
    movl %eax, screen_width
    testl %eax, %eax
    jle fatal_error
    pushl $1
    call *iat_GetSystemMetrics
    movl %eax, screen_height
    testl %eax, %eax
    jle fatal_error

    # Use the user's current desktop colour. COLORREF is 00BBGGRR;
    # GDI+ expects AARRGGBB.
    pushl $1
    call *iat_GetSysColor
    movl %eax, %ebx
    movl %ebx, %eax
    andl $0xff, %eax
    shll $16, %eax
    movl %ebx, %ecx
    andl $0xff00, %ecx
    orl %ecx, %eax
    shrl $16, %ebx
    andl $0xff, %ebx
    orl %ebx, %eax
    orl $0xff000000, %eax
    movl %eax, background_argb

    # Start GDI+ and load the original transparent PNG.
    pushl $0
    pushl $gdiplus_input
    pushl $gdiplus_token
    call *iat_GdiplusStartup
    testl %eax, %eax
    jnz fatal_error
    movl $1, gdiplus_started

    pushl $source_image
    pushl $module_path
    call *iat_GdipLoadImageFromFile
    testl %eax, %eax
    jnz cleanup_error
    pushl $source_width
    pushl source_image
    call *iat_GdipGetImageWidth
    testl %eax, %eax
    jnz cleanup_error
    pushl $source_height
    pushl source_image
    call *iat_GdipGetImageHeight
    testl %eax, %eax
    jnz cleanup_error

    # Create one opaque bitmap at the exact current screen resolution.
    pushl $target_bitmap
    pushl $0
    pushl $0x0026200a
    pushl $0
    pushl screen_height
    pushl screen_width
    call *iat_GdipCreateBitmapFromScan0
    testl %eax, %eax
    jnz cleanup_error
    pushl $graphics
    pushl target_bitmap
    call *iat_GdipGetImageGraphicsContext
    testl %eax, %eax
    jnz cleanup_error
    pushl background_argb
    pushl graphics
    call *iat_GdipGraphicsClear
    testl %eax, %eax
    jnz cleanup_error
    pushl $5
    pushl graphics
    call *iat_GdipSetInterpolationMode

    # Proportional Fit: calculate the largest rectangle that preserves the
    # PNG aspect ratio, then centre it. Integer arithmetic is sufficient for
    # wallpaper dimensions and keeps the helper independent of a C runtime.
    movl screen_width, %eax
    mull source_height
    movl %eax, %ebx
    movl screen_height, %eax
    mull source_width
    cmpl %eax, %ebx
    ja height_limited

width_limited:
    movl screen_width, %eax
    movl %eax, draw_width
    mull source_height
    xorl %edx, %edx
    divl source_width
    movl %eax, draw_height
    movl $0, draw_x
    movl screen_height, %ecx
    subl %eax, %ecx
    shrl $1, %ecx
    movl %ecx, draw_y
    jmp draw_source

height_limited:
    movl screen_height, %eax
    movl %eax, draw_height
    mull source_width
    xorl %edx, %edx
    divl source_height
    movl %eax, draw_width
    movl $0, draw_y
    movl screen_width, %ecx
    subl %eax, %ecx
    shrl $1, %ecx
    movl %ecx, draw_x

draw_source:
    pushl draw_height
    pushl draw_width
    pushl draw_y
    pushl draw_x
    pushl source_image
    pushl graphics
    call *iat_GdipDrawImageRectI
    testl %eax, %eax
    jnz cleanup_error
    pushl $0
    pushl $bmp_encoder
    pushl $output_path
    pushl target_bitmap
    call *iat_GdipSaveImageToFile
    testl %eax, %eax
    jnz cleanup_error

    call cleanup_gdiplus

    # Exact-size BMP: Center mode displays it pixel-for-pixel. Explicitly
    # disable tiling so a previous wallpaper preference cannot leak through.
    call set_center_style
    pushl $3
    pushl $output_path
    pushl $0
    pushl $20
    call *iat_SystemParametersInfoW
    testl %eax, %eax
    jz fatal_error

    cmpl $0, silent_mode
    jne success_exit
    pushl $0x40
    pushl $caption
    pushl $success_text
    pushl $0
    call *iat_MessageBoxW
success_exit:
    pushl $0
    call *iat_ExitProcess

cleanup_error:
    call cleanup_gdiplus
fatal_error:
    cmpl $0, silent_mode
    jne error_exit
    pushl $0x10
    pushl $caption
    pushl $error_text
    pushl $0
    call *iat_MessageBoxW
error_exit:
    pushl $1
    call *iat_ExitProcess

detect_silent:
    call *iat_GetCommandLineW
    movl %eax, %esi
6:
    movw (%esi), %ax
    testw %ax, %ax
    jz 7f
    cmpw $'/', %ax
    jne 8f
    cmpw $'s', 2(%esi)
    jne 8f
    cmpw $'i', 4(%esi)
    jne 8f
    cmpw $'l', 6(%esi)
    jne 8f
    cmpw $'e', 8(%esi)
    jne 8f
    cmpw $'n', 10(%esi)
    jne 8f
    cmpw $'t', 12(%esi)
    jne 8f
    movl $1, silent_mode
    ret
8:
    addl $2, %esi
    jmp 6b
7:
    ret

cleanup_gdiplus:
    cmpl $0, graphics
    je 1f
    pushl graphics
    call *iat_GdipDeleteGraphics
    movl $0, graphics
1:
    cmpl $0, target_bitmap
    je 2f
    pushl target_bitmap
    call *iat_GdipDisposeImage
    movl $0, target_bitmap
2:
    cmpl $0, source_image
    je 3f
    pushl source_image
    call *iat_GdipDisposeImage
    movl $0, source_image
3:
    cmpl $0, gdiplus_started
    je 4f
    pushl gdiplus_token
    call *iat_GdiplusShutdown
    movl $0, gdiplus_started
4:
    ret

set_center_style:
    pushl $reg_disposition
    pushl $reg_key
    pushl $0
    pushl $2
    pushl $0
    pushl $0
    pushl $0
    pushl $desktop_key
    pushl $0x80000001
    call *iat_RegCreateKeyExW
    testl %eax, %eax
    jnz 5f
    pushl $4
    pushl $zero_text
    pushl $1
    pushl $0
    pushl $wallpaper_style_name
    pushl reg_key
    call *iat_RegSetValueExW
    pushl $4
    pushl $zero_text
    pushl $1
    pushl $0
    pushl $tile_wallpaper_name
    pushl reg_key
    call *iat_RegSetValueExW
    pushl reg_key
    call *iat_RegCloseKey
5:
    ret

    .section .rdata,"a"
    .align 4
gdiplus_input:
    .long 1, 0, 0, 0
bmp_encoder:
    .long 0x557cf400
    .short 0x1a04, 0x11d3
    .byte 0x9a, 0x73, 0x00, 0x00, 0xf8, 0x1e, 0xf3, 0x2e

appdata_name: .short 'A','P','P','D','A','T','A',0
png_suffix: .short 'a','s','s','e','t','s',0x5c,'N','O','S','-','P','i','p','e','-','c','a','t','.','p','n','g',0
folder_suffix: .short 0x5c,'N','O','S','-','P','i','p','e',0
file_suffix: .short 0x5c,'N','O','S','-','P','i','p','e','-','w','a','l','l','p','a','p','e','r','.','b','m','p',0
desktop_key: .short 'C','o','n','t','r','o','l',' ','P','a','n','e','l',0x5c,'D','e','s','k','t','o','p',0
wallpaper_style_name: .short 'W','a','l','l','p','a','p','e','r','S','t','y','l','e',0
tile_wallpaper_name: .short 'T','i','l','e','W','a','l','l','p','a','p','e','r',0
zero_text: .short '0',0
caption: .short 'N','O','S','-','P','i','p','e',' ','W','a','l','l','p','a','p','e','r',0
success_text: .short 'W','a','l','l','p','a','p','e','r',' ','u','p','d','a','t','e','d',' ','f','o','r',' ','t','h','e',' ','c','u','r','r','e','n','t',' ','s','c','r','e','e','n',' ','a','n','d',' ','d','e','s','k','t','o','p',' ','c','o','l','o','r','.',0
error_text: .short 'C','o','u','l','d',' ','n','o','t',' ','c','r','e','a','t','e',' ','o','r',' ','a','p','p','l','y',' ','t','h','e',' ','N','O','S','-','P','i','p','e',' ','w','a','l','l','p','a','p','e','r','.',0

    .section .bss,"aw",@nobits
    .align 4
module_path: .space 32768
output_path: .space 32768
gdiplus_token: .long 0
gdiplus_started: .long 0
source_image: .long 0
target_bitmap: .long 0
graphics: .long 0
source_width: .long 0
source_height: .long 0
screen_width: .long 0
screen_height: .long 0
draw_x: .long 0
draw_y: .long 0
draw_width: .long 0
draw_height: .long 0
background_argb: .long 0
reg_key: .long 0
reg_disposition: .long 0
silent_mode: .long 0

    .section .idata,"aw"
    .align 4
import_descriptors:
    .long ilt_kernel32-0x400000,0,0,kernel32_name-0x400000,iat_kernel32-0x400000
    .long ilt_user32-0x400000,0,0,user32_name-0x400000,iat_user32-0x400000
    .long ilt_advapi32-0x400000,0,0,advapi32_name-0x400000,iat_advapi32-0x400000
    .long ilt_gdiplus-0x400000,0,0,gdiplus_name-0x400000,iat_gdiplus-0x400000
    .long 0,0,0,0,0

ilt_kernel32:
    .long hn_GetCommandLineW-0x400000, hn_GetModuleFileNameW-0x400000, hn_GetEnvironmentVariableW-0x400000
    .long hn_CreateDirectoryW-0x400000, hn_lstrcatW-0x400000, hn_ExitProcess-0x400000, 0
ilt_user32:
    .long hn_GetSystemMetrics-0x400000, hn_GetSysColor-0x400000
    .long hn_SystemParametersInfoW-0x400000
    .long hn_MessageBoxW-0x400000, 0
ilt_advapi32:
    .long hn_RegCreateKeyExW-0x400000, hn_RegSetValueExW-0x400000
    .long hn_RegCloseKey-0x400000, 0
ilt_gdiplus:
    .long hn_GdiplusStartup-0x400000, hn_GdiplusShutdown-0x400000
    .long hn_GdipLoadImageFromFile-0x400000, hn_GdipGetImageWidth-0x400000
    .long hn_GdipGetImageHeight-0x400000, hn_GdipCreateBitmapFromScan0-0x400000
    .long hn_GdipGetImageGraphicsContext-0x400000, hn_GdipGraphicsClear-0x400000
    .long hn_GdipSetInterpolationMode-0x400000, hn_GdipDrawImageRectI-0x400000
    .long hn_GdipSaveImageToFile-0x400000, hn_GdipDeleteGraphics-0x400000
    .long hn_GdipDisposeImage-0x400000, 0

iat_kernel32:
iat_GetCommandLineW: .long hn_GetCommandLineW-0x400000
iat_GetModuleFileNameW: .long hn_GetModuleFileNameW-0x400000
iat_GetEnvironmentVariableW: .long hn_GetEnvironmentVariableW-0x400000
iat_CreateDirectoryW: .long hn_CreateDirectoryW-0x400000
iat_lstrcatW: .long hn_lstrcatW-0x400000
iat_ExitProcess: .long hn_ExitProcess-0x400000
    .long 0
iat_user32:
iat_GetSystemMetrics: .long hn_GetSystemMetrics-0x400000
iat_GetSysColor: .long hn_GetSysColor-0x400000
iat_SystemParametersInfoW: .long hn_SystemParametersInfoW-0x400000
iat_MessageBoxW: .long hn_MessageBoxW-0x400000
    .long 0
iat_advapi32:
iat_RegCreateKeyExW: .long hn_RegCreateKeyExW-0x400000
iat_RegSetValueExW: .long hn_RegSetValueExW-0x400000
iat_RegCloseKey: .long hn_RegCloseKey-0x400000
    .long 0
iat_gdiplus:
iat_GdiplusStartup: .long hn_GdiplusStartup-0x400000
iat_GdiplusShutdown: .long hn_GdiplusShutdown-0x400000
iat_GdipLoadImageFromFile: .long hn_GdipLoadImageFromFile-0x400000
iat_GdipGetImageWidth: .long hn_GdipGetImageWidth-0x400000
iat_GdipGetImageHeight: .long hn_GdipGetImageHeight-0x400000
iat_GdipCreateBitmapFromScan0: .long hn_GdipCreateBitmapFromScan0-0x400000
iat_GdipGetImageGraphicsContext: .long hn_GdipGetImageGraphicsContext-0x400000
iat_GdipGraphicsClear: .long hn_GdipGraphicsClear-0x400000
iat_GdipSetInterpolationMode: .long hn_GdipSetInterpolationMode-0x400000
iat_GdipDrawImageRectI: .long hn_GdipDrawImageRectI-0x400000
iat_GdipSaveImageToFile: .long hn_GdipSaveImageToFile-0x400000
iat_GdipDeleteGraphics: .long hn_GdipDeleteGraphics-0x400000
iat_GdipDisposeImage: .long hn_GdipDisposeImage-0x400000
    .long 0

kernel32_name: .asciz "KERNEL32.dll"
user32_name: .asciz "USER32.dll"
advapi32_name: .asciz "ADVAPI32.dll"
gdiplus_name: .asciz "GDIPLUS.dll"
    .align 2
hn_GetModuleFileNameW: .short 0; .asciz "GetModuleFileNameW"; .align 2
hn_GetCommandLineW: .short 0; .asciz "GetCommandLineW"; .align 2
hn_GetEnvironmentVariableW: .short 0; .asciz "GetEnvironmentVariableW"; .align 2
hn_CreateDirectoryW: .short 0; .asciz "CreateDirectoryW"; .align 2
hn_ExitProcess: .short 0; .asciz "ExitProcess"; .align 2
hn_GetSystemMetrics: .short 0; .asciz "GetSystemMetrics"; .align 2
hn_GetSysColor: .short 0; .asciz "GetSysColor"; .align 2
hn_lstrcatW: .short 0; .asciz "lstrcatW"; .align 2
hn_SystemParametersInfoW: .short 0; .asciz "SystemParametersInfoW"; .align 2
hn_MessageBoxW: .short 0; .asciz "MessageBoxW"; .align 2
hn_RegCreateKeyExW: .short 0; .asciz "RegCreateKeyExW"; .align 2
hn_RegSetValueExW: .short 0; .asciz "RegSetValueExW"; .align 2
hn_RegCloseKey: .short 0; .asciz "RegCloseKey"; .align 2
hn_GdiplusStartup: .short 0; .asciz "GdiplusStartup"; .align 2
hn_GdiplusShutdown: .short 0; .asciz "GdiplusShutdown"; .align 2
hn_GdipLoadImageFromFile: .short 0; .asciz "GdipLoadImageFromFile"; .align 2
hn_GdipGetImageWidth: .short 0; .asciz "GdipGetImageWidth"; .align 2
hn_GdipGetImageHeight: .short 0; .asciz "GdipGetImageHeight"; .align 2
hn_GdipCreateBitmapFromScan0: .short 0; .asciz "GdipCreateBitmapFromScan0"; .align 2
hn_GdipGetImageGraphicsContext: .short 0; .asciz "GdipGetImageGraphicsContext"; .align 2
hn_GdipGraphicsClear: .short 0; .asciz "GdipGraphicsClear"; .align 2
hn_GdipSetInterpolationMode: .short 0; .asciz "GdipSetInterpolationMode"; .align 2
hn_GdipDrawImageRectI: .short 0; .asciz "GdipDrawImageRectI"; .align 2
hn_GdipSaveImageToFile: .short 0; .asciz "GdipSaveImageToFile"; .align 2
hn_GdipDeleteGraphics: .short 0; .asciz "GdipDeleteGraphics"; .align 2
hn_GdipDisposeImage: .short 0; .asciz "GdipDisposeImage"; .align 2
