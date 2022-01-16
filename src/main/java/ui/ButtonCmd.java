package ui;

enum ButtonCmd {
    ButtonCreate("btnCreate"),
    ButtonView("btnView"),
    ButtonSave("btnSave"),
    ButtonClear("btnClear"),
    ButtonDel("btnDel"),
    ButtonReload("btnReload"),
    ButtonJsonCompress("btnJsonCompress"),
    ButtonJsonExpand("btnJsonExpand"),
    ButtonNodeExport("btnNodeExport"),
    ;
    public final String cmd;

    ButtonCmd(String cmd) {
        this.cmd = cmd;
    }
}
