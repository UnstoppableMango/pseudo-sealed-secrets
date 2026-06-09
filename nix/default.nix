{
  jre,
  lib,
  makeWrapper,
  maven,
}:
let
  version = "0.1.0";
  jarFile = "pseudo-sealed-secrets-${version}-SNAPSHOT.jar";
in
maven.buildMavenPackage {
  pname = "pseudo-sealed-secrets";
  inherit version;

  src = lib.cleanSource ../.;
  mvnHash = "sha256-a2XYiaanHX67/QGC7Li7tQM7HbZ38x/1ntu0e525ISI=";

  nativeBuildInputs = [ makeWrapper ];

  doCheck = false;

  installPhase = ''
    mkdir -p $out/bin $out/share/pseudo-sealed-secrets
    install -Dm644 target/${jarFile} $out/share/pseudo-sealed-secrets

    makeWrapper ${jre}/bin/java $out/bin/pseudo-sealed-secrets \
      --add-flags "-jar $out/share/pseudo-sealed-secrets/${jarFile}"
  '';
}
