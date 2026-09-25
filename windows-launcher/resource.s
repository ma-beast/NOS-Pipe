    .section .rsrc,"a"
    .align 4
resource_root:
    .long 0, 0
    .short 0, 0, 0, 2
    .long 3, 0x80000000 | (icon_type_dir - resource_root)
    .long 14, 0x80000000 | (group_type_dir - resource_root)

icon_type_dir:
    .long 0, 0
    .short 0, 0, 0, 7
    .long 1, 0x80000000 | (icon_lang_1 - resource_root)
    .long 2, 0x80000000 | (icon_lang_2 - resource_root)
    .long 3, 0x80000000 | (icon_lang_3 - resource_root)
    .long 4, 0x80000000 | (icon_lang_4 - resource_root)
    .long 5, 0x80000000 | (icon_lang_5 - resource_root)
    .long 6, 0x80000000 | (icon_lang_6 - resource_root)
    .long 7, 0x80000000 | (icon_lang_7 - resource_root)

icon_lang_1: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_1 - resource_root
icon_lang_2: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_2 - resource_root
icon_lang_3: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_3 - resource_root
icon_lang_4: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_4 - resource_root
icon_lang_5: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_5 - resource_root
icon_lang_6: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_6 - resource_root
icon_lang_7: .long 0,0; .short 0,0,0,1; .long 1033, icon_data_entry_7 - resource_root

group_type_dir:
    .long 0, 0
    .short 0, 0, 0, 1
    .long 1, 0x80000000 | (group_lang - resource_root)
group_lang:
    .long 0, 0
    .short 0, 0, 0, 1
    .long 1033, group_data_entry - resource_root

icon_data_entry_1: .long icon_data_1 - 0x400000, 1128,   0, 0
icon_data_entry_2: .long icon_data_2 - 0x400000, 2440,   0, 0
icon_data_entry_3: .long icon_data_3 - 0x400000, 4264,   0, 0
icon_data_entry_4: .long icon_data_4 - 0x400000, 9640,   0, 0
icon_data_entry_5: .long icon_data_5 - 0x400000, 16936,  0, 0
icon_data_entry_6: .long icon_data_6 - 0x400000, 67624,  0, 0
icon_data_entry_7: .long icon_data_7 - 0x400000, 270376, 0, 0
group_data_entry:  .long group_data  - 0x400000, 104,     0, 0

    .align 4
group_data:
    .short 0, 1, 7
    .byte 16, 16, 0, 0;   .short 1, 32; .long 1128;   .short 1
    .byte 24, 24, 0, 0;   .short 1, 32; .long 2440;   .short 2
    .byte 32, 32, 0, 0;   .short 1, 32; .long 4264;   .short 3
    .byte 48, 48, 0, 0;   .short 1, 32; .long 9640;   .short 4
    .byte 64, 64, 0, 0;   .short 1, 32; .long 16936;  .short 5
    .byte 128,128,0, 0;   .short 1, 32; .long 67624;  .short 6
    .byte 0, 0, 0, 0;     .short 1, 32; .long 270376; .short 7

    .align 4
icon_data_1: .incbin "icon0.bin"; .align 4
icon_data_2: .incbin "icon1.bin"; .align 4
icon_data_3: .incbin "icon2.bin"; .align 4
icon_data_4: .incbin "icon3.bin"; .align 4
icon_data_5: .incbin "icon4.bin"; .align 4
icon_data_6: .incbin "icon5.bin"; .align 4
icon_data_7: .incbin "icon6.bin"; .align 4
