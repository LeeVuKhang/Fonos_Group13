package com.example.fonos_group13.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

class FirebaseSignedInUserProvider implements SignedInUserProvider {
    private final FirebaseAuth auth;

    FirebaseSignedInUserProvider(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public String currentUid() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }
}
