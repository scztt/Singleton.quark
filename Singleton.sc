Singleton {
	classvar all, <>know=false, creatingNew=false;
	var <>name, <>definitionFilename;

	*initClass {
		all = IdentityDictionary();
	}

	*default {
		^\default
	}

	*all {
		^all[this] ?? IdentityDictionary()
	}

	*at {
		|name|
		^this.new(name);
	}

	*put {
		|name, value|
		^this.new(name, value)
	}

	*removeAt {
		|key|
		^this.clearItem(key)
	}

	*new {
		arg name ...settings;
		var sing, classAll, created=false;

		name = this.makeName(name);

		classAll = all.atFail(this, {
			all[this] = IdentityDictionary();
			all[this];
		});

		sing = classAll.atFail(name, {
			var newSingleton = this.createNew();
			created = true;
			newSingleton.name = name;
			classAll[name] = newSingleton;
			newSingleton.init(name);
			newSingleton;
		});

		if ((settings.notNil && settings.notEmpty) || created) {
			sing.set(*settings);
			sing.definitionFilename = thisProcess.nowExecutingPath.asSymbol;
		};

		if (created) { { this.changed(\added, sing) }.defer(0) };

		^sing;
	}

	*createNew {
		arg ...args;
		^super.new(*args);
	}

	*doesNotUnderstand { arg selector ... args;
		var item;

		if (know && creatingNew.not) {
			creatingNew = true;		// avoid reentrancy
			protect {
				if (selector.isSetter) {
					selector = selector.asString;
					selector = selector[0..(selector.size - 2)].asSymbol;
					item = this.new(selector, *args);
				} {
					item = this.new(selector);
				}
			} {
				creatingNew = false;
			};

			^item;
		} {
			^this.superPerformList(\doesNotUnderstand, selector, args);
		}
	}

	*use {
		|function|
		var envir, singleton;

		this.beforeUse();
		protect {
			EnvironmentRedirect(this).use(function)
		} {
			|e|
			this.afterUse();
		}
	}

	*makeName {
		|selector|
		if (selector.isKindOf(String)) {
			selector = selector.asSymbol;
		};
		if (selector.isKindOf(Array)) {
			selector = selector.join("_").asSymbol;
		};

		^(selector ?? { this.default })
	}

	// Overridable interfaces
	*beforeUse {}
	*afterUse {}

	init {
		|name|
	}

	set {
		|...settings|
		// Override this to receive 'settings' parameter from Singleton.new(name, settings)
	}

	*clearItem {
		|sing|
		var dict = all[this];
		if (dict.notNil) {
			var key = dict.findKeyForValue(sing);
			if (key.notNil) {
				dict[key] = nil;
				this.changed(\removed, sing);
			}
		}
	}

	*clear {
		var dict = all[this];
		if (dict.notNil) {
			dict.values.do(_.clear())
		}
	}

	clear {
		this.class.clearItem(this);
	}

	printOn {
		|stream|
		stream << "%('%')".format(this.class.name, name)
	}
}