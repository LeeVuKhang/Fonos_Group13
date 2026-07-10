package com.example.fonos_group13;

public final class FonosTestApplication extends FonosApplication {
    @Override
    protected AppContainer createAppContainer() {
        return new TestAppContainer(this);
    }
}
