package com.snapcardster.omnimtg.android;

import com.snapcardster.omnimtg.MainController;

public class MainControllerWrapper {
    AndroidPropertyFactory propFactory = new AndroidPropertyFactory();
    AndroidNativeFunctionProvider nativeProvider = new AndroidNativeFunctionProvider();
    MainController controller = controller = new MainController(propFactory,nativeProvider);

    void loginSnap(){
        controller.loginSnap();
    }
}
