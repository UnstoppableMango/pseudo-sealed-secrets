{
  description = "A Nix flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    systems.url = "github:nix-systems/default";

    flake-parts = {
      url = "github:hercules-ci/flake-parts";
      inputs.nixpkgs-lib.follows = "nixpkgs";
    };

    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    nix2container = {
      url = "github:nlewo/nix2container";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      imports = [ inputs.treefmt-nix.flakeModule ];

      perSystem =
        { pkgs, inputs', ... }:
        let
          operator = pkgs.callPackage ./nix/default.nix { };
          n2c = inputs'.nix2container.packages.nix2container;
        in
        {
          packages.default = operator;

          packages.image = n2c.buildImage {
            name = "pseudo-sealed-secrets";
            tag = "latest";
            config.Entrypoint = [ "${operator}/bin/pseudo-sealed-secrets" ];
          };

          devShells.default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              gnumake
	      kind
	      maven
              nixfmt
	      podman
	      skopeo
            ];
          };

          treefmt.programs = {
            nixfmt.enable = true;
          };
        };
    };
}
