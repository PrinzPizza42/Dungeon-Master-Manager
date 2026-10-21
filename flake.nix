{
  description = "Minimal Kotlin Multiplatform dev environment with JDK 21 and Compose dependencies";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    nixgl.url = "github:nix-community/nixGL";
  };

  outputs = { self, nixpkgs, flake-utils, nixgl }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
          overlays = [ nixgl.overlay ];
        };
        jdk = pkgs.jdk21;
        gradle = pkgs.gradle.override { java = jdk; };
      in
      {
        # Changed from 'nativeDevShells' to standard 'devShells'
        devShells.default = pkgs.mkShell rec {
          name = "kotlin-multiplatform-env";

          buildInputs = with pkgs; [
            jdk
            gradle
            kotlin
            git

            # Native libs for Compose Desktop
            libGL
            libGLU
            xorg.libX11
            xorg.libXext
            xorg.libXrender
            xorg.libXtst
            xorg.libXi
            gtk3
            glib
            pango
            cairo
            freetype
            fontconfig

            # Required for GTK file dialogs
            gsettings-desktop-schemas
            glib-networking

            # Helper tools
            unzip
            zip
          ];

          # --- THE FIX ---
          # This constructs a path string of all /lib folders in your buildInputs
          # so the JVM can find libGL.so.1 and others.
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath buildInputs;

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk}/lib/openjdk
            export PATH=$JAVA_HOME/bin:$PATH
            export _JAVA_AWT_WM_NONREPARENTING=1
            
            # Ensure GSettings schemas are found
            export XDG_DATA_DIRS=${pkgs.gsettings-desktop-schemas}/share/gsettings-schemas/${pkgs.gsettings-desktop-schemas.name}:${pkgs.gtk3}/share/gsettings-schemas/${pkgs.gtk3.name}:$XDG_DATA_DIRS

            echo "Kotlin Compose Environment Loaded"
            echo "If you have GPU issues, try running: run-gpu"

            # Alias to run gradle wrapped in nixGL
            alias run-gpu="nixGL ./gradlew run"
          '';
        };
      });
}
