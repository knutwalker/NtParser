# Change Log

## Versioning

Starting with `1.0`, this project will follow [Semantic Versioning](http://semver.org/).
While on version `0.x`, breaking and incompatible changed may be introduced
by a `MINOR` version bump as well. More precisely, semantic versioning is shifted
one version to the right, so it is considered `0.MAJOR.MINOR`.


## [Unreleased][unreleased]

## [0.3.0][0.3.0] - 2015-02-01
### Added
- Typeclass for pluggable model backends
- Artifacts for different model backends (nt and jena)

## [0.2.0][0.2.0] - 2014-01-31
### Added
- Lenient parsing, that accepts long quotes ('"""'), which are part of
  the Turtle spec, but not part of the N-Triple spec

## [0.1.0][0.1.0] - 2014-09-18
### Added
- Spec-conforming N-Triples parser


[unreleased]: https://github.com/knutwalker/ntparser/compare/v0.3.0...develop
[0.3.0]: https://github.com/knutwalker/ntparser/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/knutwalker/ntparser/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/knutwalker/ntparser/compare/2a2269a...v0.1.0
