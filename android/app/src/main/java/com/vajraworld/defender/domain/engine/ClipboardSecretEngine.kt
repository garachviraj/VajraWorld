package com.vajraworld.defender.domain.engine

import java.util.Locale
import kotlin.math.ln

data class LocalClipboardResult(
    val isSensitive: Boolean,
    val detectedTypes: List<String>,
    val riskScore: Int,
    val recommendation: String,
    val suggestedClearTimerSec: Int,
    val valueStored: Boolean = false
)

object ClipboardSecretEngine {

    // Regex patterns for secrets
    private val AWS_KEY_REGEX = Regex("""\b(AKIA[0-9A-Z]{16})\b""")
    private val GITHUB_TOKEN_REGEX = Regex("""\b(ghp_[0-9a-zA-Z]{36}|github_pat_[0-9a-zA-Z_]{22,82})\b""")
    private val SLACK_TOKEN_REGEX = Regex("""\b(xox[baprs]-[0-9]{10,13}-[0-9]{10,13}-[a-zA-Z0-9]{24,32}|xox[baprs]-[0-9a-zA-Z-]{10,72})\b""")
    private val PEM_PRIVATE_KEY_REGEX = Regex("""-----BEGIN (?:[A-Z0-9_-]+ )?PRIVATE KEY-----""")
    private val JWT_REGEX = Regex("""\beyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b""")

    // Credit card candidate pattern: 13-19 digits with optional spaces or dashes
    private val CC_CANDIDATE_REGEX = Regex("""\b(?:\d[ -]*){12,18}\d\b""")


    // Subset of BIP-39 English words for crypto mnemonic detection
    private val BIP39_SEEDS = hashSetOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse",
        "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
        "action", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust", "admit",
        "adult", "advance", "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
        "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol", "alert",
        "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also", "alter",
        "always", "amateur", "amazing", "among", "amount", "amused", "analyst", "anchor", "ancient", "anger",
        "angle", "angry", "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique",
        "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april", "arch", "arctic",
        "area", "arena", "argue", "arm", "armed", "armor", "army", "around", "arrange", "arrest",
        "arrive", "arrow", "art", "artefact", "artist", "artwork", "ask", "aspect", "assault", "asset",
        "assist", "assume", "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction",
        "audit", "august", "aunt", "author", "auto", "autumn", "average", "avocado", "avoid", "awake",
        "aware", "away", "awesome", "awful", "awkward", "axis", "baby", "bachelor", "bacon", "badge",
        "bag", "balance", "balcony", "ball", "bamboo", "banana", "banner", "bar", "barely", "bargain",
        "barrel", "base", "basic", "basket", "battle", "beach", "bean", "beauty", "because", "become",
        "beef", "before", "begin", "behave", "behind", "believe", "below", "belt", "bench", "benefit",
        "best", "betray", "better", "between", "beyond", "bicycle", "bid", "bike", "bind", "biology",
        "bird", "birth", "bitter", "black", "blade", "blame", "blanket", "blast", "bleak", "bless",
        "blind", "blood", "blossom", "blouse", "blue", "blur", "blush", "board", "boat", "body",
        "boil", "bomb", "bone", "bonus", "book", "boost", "border", "boring", "borrow", "boss",
        "bottom", "bounce", "box", "boy", "bracket", "brain", "brand", "brass", "brave", "bread",
        "breeze", "brick", "bridge", "brief", "bright", "bring", "brisk", "broccoli", "broken", "bronze",
        "broom", "brother", "brown", "brush", "bubble", "buddy", "budget", "buffalo", "build", "bulb",
        "bulk", "bullet", "bundle", "bunker", "burden", "burger", "burst", "bus", "business", "busy",
        "butter", "buyer", "buzz", "cabbage", "cabin", "cable", "cactus", "cage", "cake", "call",
        "calm", "camera", "camp", "can", "canal", "cancel", "candy", "cannon", "canoe", "canvas",
        "canyon", "capable", "capital", "captain", "car", "carbon", "card", "cargo", "carpet", "carry",
        "cart", "case", "cash", "casino", "castle", "casual", "cat", "catalog", "catch", "category",
        "cattle", "caught", "cause", "caution", "cave", "ceiling", "celery", "cement", "census", "century",
        "cereal", "certain", "chair", "chalk", "champion", "change", "chaos", "chapter", "charge", "chase",
        "chat", "cheap", "check", "cheese", "chef", "cherry", "chest", "chicken", "chief", "child",
        "chimney", "choice", "choose", "chronic", "chuckle", "chunk", "churn", "cigar", "cinnamon", "circle",
        "citizen", "city", "civil", "claim", "clap", "clarify", "claw", "clay", "clean", "clerk",
        "clever", "click", "client", "cliff", "climb", "clinic", "clip", "clock", "clog", "close",
        "cloth", "cloud", "clown", "club", "clump", "cluster", "clutch", "coach", "coast", "coconut",
        "code", "coffee", "coil", "coin", "collect", "color", "column", "combine", "come", "comfort",
        "comic", "common", "company", "concert", "conduct", "confirm", "congress", "connect", "consider", "control",
        "convince", "cook", "cool", "copper", "copy", "coral", "core", "corn", "correct", "cost",
        "cotton", "couch", "country", "couple", "course", "cousin", "cover", "coyote", "crack", "cradle",
        "craft", "cram", "crane", "crash", "crater", "crawl", "crazy", "cream", "credit", "creek",
        "crew", "cricket", "crime", "crisp", "critic", "crop", "cross", "crouch", "crowd", "crucial",
        "cruel", "cruise", "crumble", "crunch", "crush", "cry", "crystal", "cube", "culture", "cup",
        "cupboard", "curious", "current", "curtain", "curve", "cushion", "custom", "cute", "cycle", "dad",
        "damage", "damp", "dance", "danger", "daring", "dash", "daughter", "dawn", "day", "deal",
        "debate", "debris", "decade", "december", "decide", "decline", "decorate", "decrease", "deer", "defense",
        "define", "defy", "degree", "delay", "deliver", "demand", "demise", "denial", "dentist", "deny",
        "depart", "depend", "deposit", "depth", "deputy", "derive", "describe", "desert", "design", "desk",
        "despair", "destroy", "detail", "detect", "develop", "device", "devote", "diagram", "dial", "diamond",
        "diary", "dice", "diesel", "diet", "differ", "digital", "dignity", "dilemma", "dinner", "dinosaur",
        "direct", "dirt", "disagree", "discover", "disease", "dish", "dismiss", "disorder", "display", "distance",
        "divert", "divide", "divorce", "dizzy", "doctor", "document", "dog", "doll", "dolphin", "domain",
        "donate", "donkey", "donor", "door", "dose", "double", "dove", "draft", "dragon", "drain",
        "drama", "drastic", "draw", "dream", "dress", "drift", "drill", "drink", "drip", "drive",
        "drop", "drum", "dry", "duck", "dumb", "dune", "during", "dust", "dutch", "duty",
        "dwarf", "dynamic", "eager", "eagle", "early", "earn", "earth", "easily", "east", "easy",
        "echo", "ecology", "economy", "edge", "edit", "educate", "effort", "egg", "eight", "either",
        "elbow", "elder", "electric", "elegant", "element", "elephant", "elevator", "elite", "else", "embark",
        "embody", "embrace", "emerge", "emotion", "employ", "empower", "empty", "enable", "enact", "end",
        "endless", "endorse", "enemy", "energy", "enforce", "engage", "engine", "enhance", "enjoy", "enlist",
        "enough", "enrich", "enroll", "ensure", "enter", "entire", "entry", "envelope", "episode", "equal",
        "equip", "era", "erase", "erode", "erosion", "error", "erupt", "escape", "essay", "essence",
        "estate", "eternal", "ethics", "evidence", "evil", "evoke", "evolve", "exact", "example", "excess",
        "exchange", "excite", "exclude", "excuse", "execute", "exercise", "exhaust", "exhibit", "exile", "exist",
        "exit", "exotic", "expand", "expect", "expire", "explain", "expose", "express", "extend", "extra",
        "eye", "eyebrow", "fabric", "face", "faculty", "fade", "faint", "faith", "fall", "false",
        "fame", "family", "famous", "fan", "fancy", "fantasy", "farm", "fashion", "fat", "fatal",
        "father", "fatigue", "fault", "favorite", "feature", "february", "federal", "fee", "feed", "feel",
        "female", "fence", "festival", "fetch", "fever", "few", "fiber", "fiction", "field", "figure",
        "file", "film", "filter", "final", "find", "fine", "finger", "finish", "fire", "firm",
        "first", "fiscal", "fish", "fit", "fitness", "fix", "flag", "flame", "flash", "flat",
        "flavor", "flee", "flight", "flip", "float", "flock", "floor", "flower", "fluid", "flush",
        "fly", "foam", "focus", "fog", "foil", "fold", "follow", "food", "foot", "force",
        "forest", "forget", "fork", "fortune", "forward", "found", "fox", "fragile", "frame", "frequent",
        "fresh", "friend", "fringe", "frog", "front", "frost", "frown", "frozen", "fruit", "fuel",
        "fun", "funny", "furnace", "fury", "future", "gadget", "gain", "galaxy", "gallery", "game",
        "gap", "garage", "garbage", "garden", "garlic", "gas", "gasp", "gate", "gather", "gauge",
        "gaze", "general", "genius", "genre", "gentle", "genuine", "gesture", "ghost", "giant", "gift",
        "giggle", "ginger", "giraffe", "girl", "give", "glad", "glance", "glare", "glass", "glide",
        "glimpse", "globe", "gloom", "glory", "glove", "glow", "glue", "goat", "goddess", "gold",
        "good", "goose", "gorilla", "gospel", "gossip", "govern", "gown", "grab", "grace", "grain",
        "grant", "grape", "grass", "gravity", "great", "green", "grid", "grief", "grit", "grocery",
        "group", "grow", "grunt", "guard", "guess", "guide", "guilt", "guitar", "gun", "gym",
        "habit", "hair", "half", "hammer", "hamster", "hand", "happy", "harbor", "hard", "harsh",
        "harvest", "hat", "have", "hawk", "hazard", "head", "health", "heart", "heavy", "hedgehog",
        "height", "hello", "helmet", "help", "hen", "hero", "hidden", "high", "hill", "hint",
        "hip", "hire", "history", "hobby", "hockey", "hold", "hole", "holiday", "hollow", "home",
        "honey", "hood", "hope", "horn", "horror", "horse", "hospital", "host", "hotel", "hour",
        "hover", "hub", "huge", "human", "humble", "humor", "hundred", "hungry", "hunt", "hurdle",
        "hurry", "hurt", "husband", "hybrid", "ice", "icon", "idea", "identify", "idle", "ignore",
        "ill", "illegal", "illness", "image", "imitate", "immense", "immune", "impact", "impose", "improve",
        "impulse", "inch", "include", "income", "increase", "index", "indicate", "indoor", "industry", "infant",
        "inflict", "inform", "inhale", "inherit", "initial", "inject", "injury", "inmate", "inner", "innocent",
        "input", "inquiry", "insane", "insect", "inside", "inspire", "install", "intact", "interest", "into",
        "invest", "invite", "involve", "iron", "island", "isolate", "issue", "item", "ivory", "jacket",
        "jaguar", "jar", "jazz", "jealous", "jeans", "jelly", "jewel", "job", "join", "joke",
        "journey", "joy", "judge", "juice", "jump", "jungle", "junior", "junk", "just", "kangaroo",
        "keen", "keep", "ketchup", "key", "kick", "kid", "kidney", "kind", "kingdom", "kiss",
        "kit", "kitchen", "kite", "kitten", "kiwi", "knee", "knife", "knock", "know", "lab",
        "label", "labor", "ladder", "lady", "lake", "lamp", "language", "laptop", "large", "later",
        "latin", "laugh", "laundry", "lava", "law", "lawn", "lawsuit", "layer", "lazy", "leader",
        "leaf", "learn", "leave", "lecture", "left", "leg", "legal", "legend", "leisure", "lemon",
        "lend", "length", "lens", "leopard", "lesson", "letter", "level", "liar", "liberty", "library",
        "license", "life", "lift", "light", "like", "limb", "limit", "link", "lion", "liquid",
        "list", "little", "live", "lizard", "load", "loan", "lobster", "local", "lock", "logic",
        "lonely", "long", "loop", "lottery", "loud", "lounge", "love", "loyal", "lucky", "luggage",
        "lumber", "lunar", "lunch", "luxury", "lyrics", "machine", "mad", "magic", "magnet", "maid",
        "mail", "main", "major", "make", "mammal", "man", "manage", "mandate", "mango", "mansion",
        "manual", "maple", "marble", "march", "margin", "marine", "market", "marriage", "mask", "mass",
        "master", "match", "material", "math", "matrix", "matter", "maximum", "maze", "meadow", "mean",
        "measure", "meat", "mechanic", "medal", "media", "melody", "melt", "member", "memory", "mention",
        "menu", "mercy", "merge", "merit", "merry", "mesh", "message", "metal", "method", "middle",
        "midnight", "milk", "million", "mimic", "mind", "minimum", "minor", "minute", "miracle", "mirror",
        "misery", "miss", "mistake", "mix", "mixed", "mixture", "mobile", "model", "modify", "mom",
        "moment", "monitor", "monkey", "monster", "month", "moon", "moral", "more", "morning", "mosquito",
        "mother", "motion", "motor", "mountain", "mouse", "move", "movie", "much", "muffin", "mule",
        "multiply", "muscle", "museum", "mushroom", "music", "must", "mutual", "myself", "mystery", "myth",
        "naive", "name", "napkin", "narrow", "nasty", "nation", "nature", "near", "neck", "need",
        "negative", "neglect", "neither", "nephew", "nerve", "nest", "net", "network", "neutral", "never",
        "news", "next", "nice", "night", "noble", "noise", "nominee", "noodle", "normal", "north",
        "nose", "notable", "note", "nothing", "notice", "novel", "now", "nuclear", "number", "nurse",
        "nut", "oak", "obey", "object", "oblige", "obscure", "observe", "obtain", "obvious", "occur",
        "ocean", "october", "odor", "off", "offer", "office", "often", "oil", "okay", "old",
        "olive", "olympic", "omit", "once", "one", "onion", "online", "only", "open", "opera",
        "opinion", "oppose", "option", "orange", "orbit", "orchard", "order", "ordinary", "organ", "orient",
        "original", "orphan", "ostrich", "other", "outdoor", "outer", "output", "outside", "oval", "oven",
        "over", "own", "owner", "oxygen", "oyster", "ozone", "pact", "paddle", "page", "pair",
        "palace", "palm", "panda", "panel", "panic", "panther", "paper", "parade", "parent", "park",
        "parrot", "party", "pass", "patch", "path", "patient", "patrol", "pattern", "pause", "pave",
        "payment", "peace", "peach", "peacock", "peak", "peanut", "pear", "peasant", "pelican", "pen",
        "penalty", "pencil", "people", "pepper", "perfect", "permit", "person", "pet", "phone", "photo",
        "phrase", "physical", "piano", "picnic", "picture", "piece", "pig", "pigeon", "pill", "pilot",
        "pink", "pioneer", "pipe", "pistol", "pitch", "pizza", "place", "planet", "plastic", "plate",
        "play", "please", "pledge", "pluck", "plug", "plunge", "poem", "poet", "point", "polar",
        "pole", "police", "pond", "pony", "pool", "popular", "portion", "position", "possible", "post",
        "potato", "pottery", "poverty", "powder", "power", "practice", "praise", "predict", "prefer", "prepare",
        "present", "pretty", "prevent", "price", "pride", "primary", "print", "priority", "prison", "private",
        "prize", "problem", "process", "produce", "profit", "program", "project", "promote", "proof", "property",
        "prosper", "protect", "proud", "provide", "public", "pudding", "pull", "pulp", "pulse", "pumpkin",
        "punch", "pupil", "puppy", "purchase", "purity", "purpose", "purse", "push", "put", "puzzle",
        "pyramid", "quality", "quantum", "quarter", "question", "quick", "quit", "quiz", "quote", "rabbit",
        "raccoon", "race", "rack", "radar", "radio", "rail", "rain", "raise", "rally", "ramp",
        "ranch", "random", "range", "rapid", "rare", "rate", "rather", "raven", "raw", "razor",
        "ready", "real", "reason", "rebel", "rebuild", "recall", "receive", "recipe", "record", "recycle",
        "reduce", "reflect", "reform", "refuse", "region", "regret", "regular", "reject", "relax", "release",
        "relief", "rely", "remain", "remember", "remind", "remove", "render", "renew", "rent", "reopen",
        "repair", "repeat", "replace", "report", "require", "rescue", "resemble", "resist", "resource", "response",
        "result", "retire", "retreat", "return", "reunion", "reveal", "review", "reward", "rhythm", "rib",
        "ribbon", "rice", "rich", "ride", "ridge", "rifle", "right", "rigid", "ring", "riot",
        "ripple", "risk", "ritual", "rival", "river", "road", "roast", "robot", "robust", "rocket",
        "romance", "roof", "rookie", "room", "rose", "rotate", "rough", "round", "route", "royal",
        "rubber", "rude", "rug", "rule", "run", "runway", "rural", "sad", "saddle", "sadness",
        "safe", "sail", "salad", "salmon", "salon", "salt", "salute", "same", "sample", "sand",
        "satisfy", "satoshi", "sauce", "sausage", "save", "say", "scale", "scan", "scare", "scatter",
        "scene", "scheme", "school", "science", "scissors", "scorpion", "scout", "scrap", "screen", "script",
        "scrub", "sea", "search", "season", "seat", "second", "secret", "section", "security", "seed",
        "seek", "segment", "select", "sell", "seminar", "senior", "sense", "sentence", "series", "service",
        "session", "settle", "setup", "seven", "shadow", "shaft", "shallow", "share", "shed", "shell",
        "sheriff", "shield", "shift", "shine", "ship", "shiver", "shock", "shoe", "shoot", "shop",
        "short", "shoulder", "shove", "shrimp", "shrug", "shuffle", "shy", "sibling", "sick", "side",
        "siege", "sight", "sign", "silent", "silk", "silly", "silver", "similar", "simple", "since",
        "sing", "siren", "sister", "situate", "six", "size", "skate", "sketch", "ski", "skill",
        "skin", "skirt", "skull", "slab", "slam", "sleep", "slender", "slice", "slide", "slight",
        "slim", "slogan", "slot", "slow", "slush", "small", "smart", "smile", "smoke", "smooth",
        "snack", "snake", "snap", "sniff", "snow", "soap", "soccer", "social", "sock", "soda",
        "soft", "solar", "soldier", "solid", "solution", "solve", "someone", "song", "soon", "sorry",
        "sort", "soul", "sound", "soup", "source", "south", "space", "spare", "spatial", "spawn",
        "speak", "special", "speed", "spell", "spend", "sphere", "spice", "spider", "spike", "spin",
        "spirit", "split", "spoil", "sponsor", "spoon", "sport", "spot", "spray", "spread", "spring",
        "spy", "square", "squeeze", "squirrel", "stable", "stadium", "staff", "stage", "stairs", "stamp",
        "stand", "start", "state", "stay", "steak", "steel", "stem", "step", "stereo", "stick",
        "still", "sting", "stock", "stomach", "stone", "stool", "story", "stove", "strategy", "street",
        "strike", "strong", "struggle", "student", "stuff", "stumble", "style", "subject", "submit", "subway",
        "success", "such", "sudden", "suffer", "sugar", "suggest", "suit", "summer", "sun", "sunny",
        "sunset", "super", "supply", "supreme", "sure", "surface", "surge", "surprise", "surround", "survey",
        "suspect", "sustain", "swallow", "swamp", "swap", "swarm", "swear", "sweet", "swift", "swim",
        "swing", "switch", "sword", "symbol", "symptom", "syrup", "system", "table", "tackle", "tag",
        "tail", "talent", "talk", "tank", "tape", "target", "task", "taste", "tattoo", "taxi",
        "teach", "team", "tell", "ten", "tenant", "tennis", "tent", "term", "test", "text",
        "thank", "that", "theme", "then", "theory", "there", "they", "thing", "this", "thought",
        "three", "thrive", "throw", "thumb", "thunder", "ticket", "tide", "tiger", "tilt", "timber",
        "time", "tiny", "tip", "tired", "tissue", "title", "toast", "tobacco", "today", "toddler",
        "toe", "together", "toilet", "token", "tomato", "tomorrow", "tone", "tongue", "tonight", "tool",
        "tooth", "top", "topic", "topple", "torch", "tornado", "tortoise", "toss", "total", "tourist",
        "toward", "tower", "town", "toy", "track", "trade", "traffic", "train", "transfer", "trap",
        "trash", "travel", "tray", "treat", "tree", "trend", "trial", "tribe", "trick", "trigger",
        "trim", "trip", "trophy", "trouble", "truck", "true", "truly", "trumpet", "trust", "truth",
        "try", "tube", "tuition", "tumble", "tuna", "tunnel", "turkey", "turn", "turtle", "twelve",
        "twenty", "twice", "twin", "twist", "two", "type", "typical", "ugly", "umbrella", "unable",
        "unaware", "uncle", "uncover", "under", "undo", "unfair", "unfold", "unhappy", "uniform", "unique",
        "unit", "universe", "unknown", "unlock", "until", "unusual", "unveil", "update", "upgrade", "uphold",
        "upon", "upper", "upset", "urban", "urge", "usage", "use", "used", "useful", "useless",
        "usual", "utility", "vacant", "vacuum", "vague", "valuable", "valve", "van", "vanish", "vapor",
        "various", "vast", "vault", "vegetable", "vehicle", "velvet", "vendor", "venture", "venue", "verb",
        "verify", "version", "very", "vessel", "veteran", "viable", "vibrant", "vicious", "victory", "video",
        "view", "village", "vintage", "violin", "virtual", "virus", "visa", "visit", "visual", "vital",
        "vivid", "vocal", "voice", "void", "volcano", "volume", "vote", "voyage", "wage", "wagon",
        "wait", "walk", "wall", "walnut", "want", "war", "warm", "warrior", "wash", "wasp",
        "waste", "water", "wave", "way", "wealth", "weapon", "wear", "weasel", "weather", "web",
        "wedding", "week", "weird", "welcome", "west", "wet", "whale", "what", "wheat", "wheel",
        "when", "where", "whip", "whisper", "wide", "width", "wife", "wild", "will", "win",
        "window", "wine", "wing", "wink", "winner", "winter", "wire", "wisdom", "wise", "wish",
        "witness", "wolf", "woman", "wonder", "wood", "wool", "word", "work", "world", "worry",
        "worth", "wrap", "wreck", "wrestle", "wrist", "write", "wrong", "yard", "year", "yellow",
        "you", "young", "youth", "zebra", "zero", "zone", "zoo"
    )

    /**
     * Genuine Luhn checksum algorithm implementation.
     */
    fun isValidLuhn(numberString: String): Boolean {
        val clean = numberString.filter { it.isDigit() }
        if (clean.length < 2) return false

        var sum = 0
        var alternate = false
        for (i in clean.length - 1 downTo 0) {
            var n = clean[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }


    private fun calculateShannonEntropy(text: String): Float {
        if (text.isEmpty()) return 0f
        val map = HashMap<Char, Int>()
        for (c in text) map[c] = (map[c] ?: 0) + 1
        val len = text.length.toDouble()
        val log2 = ln(2.0)
        var ent = 0.0
        for ((_, cnt) in map) {
            val p = cnt / len
            ent -= p * (ln(p) / log2)
        }
        return ent.toFloat()
    }

    /**
     * Check if text contains a cryptocurrency mnemonic seed phrase (12, 15, 18, 21, or 24 words).
     */
    private fun detectMnemonicSeedPhrase(text: String): Boolean {
        val words = text.trim().split(Regex("""\s+""")).map { it.lowercase(Locale.ROOT) }
        val validCounts = setOf(12, 15, 18, 21, 24)
        if (!validCounts.contains(words.size)) return false

        // Check how many words belong to BIP-39 dictionary
        var bipCount = 0
        for (w in words) {
            if (BIP39_SEEDS.contains(w)) {
                bipCount++
            }
        }
        val ratio = bipCount.toFloat() / words.size.toFloat()
        return ratio >= 0.80f // 80%+ match to BIP-39 standard vocabulary
    }

    /**
     * Inspect text from foreground clipboard without persisting or logging the content.
     * STRICT PRIVACY RULE: NEVER stores or logs the actual secret text. `valueStored = false`.
     */
    fun scan(clipboardText: String?): LocalClipboardResult = analyze(clipboardText)

    fun analyze(clipboardText: String?): LocalClipboardResult {
        if (clipboardText.isNullOrBlank()) {
            return LocalClipboardResult(
                isSensitive = false,
                detectedTypes = emptyList(),
                riskScore = 0,
                recommendation = "Clipboard is clear.",
                suggestedClearTimerSec = 0,
                valueStored = false
            )
        }

        val detected = mutableListOf<String>()
        var highestRisk = 0

        // 1. AWS Access Key
        if (AWS_KEY_REGEX.containsMatchIn(clipboardText)) {
            detected.add("AWS Access Key ID")
            highestRisk = maxOf(highestRisk, 95)
        }

        // 2. GitHub Token
        if (GITHUB_TOKEN_REGEX.containsMatchIn(clipboardText)) {
            detected.add("GitHub Personal Access Token")
            highestRisk = maxOf(highestRisk, 95)
        }

        // 3. Slack API Token
        if (SLACK_TOKEN_REGEX.containsMatchIn(clipboardText)) {
            detected.add("Slack Bot/User API Token")
            highestRisk = maxOf(highestRisk, 90)
        }

        // 4. PEM Private Key
        if (PEM_PRIVATE_KEY_REGEX.containsMatchIn(clipboardText)) {
            detected.add("RSA/EC/DSA Private Key")
            highestRisk = maxOf(highestRisk, 100)
        }

        // 5. Credit Card numbers (Luhn proxy check)
        val ccCandidates = CC_CANDIDATE_REGEX.findAll(clipboardText)
        for (match in ccCandidates) {
            val digitsOnly = match.value.filter { it.isDigit() }
            if (isValidLuhn(digitsOnly)) {
                val cardIssuer = when {
                    digitsOnly.startsWith("4") -> "Visa"
                    digitsOnly.startsWith("51") || digitsOnly.startsWith("52") ||
                    digitsOnly.startsWith("53") || digitsOnly.startsWith("54") ||
                    digitsOnly.startsWith("55") -> "MasterCard"
                    digitsOnly.startsWith("34") || digitsOnly.startsWith("37") -> "American Express"
                    digitsOnly.startsWith("6011") || digitsOnly.startsWith("65") -> "Discover"
                    else -> "Payment Card"
                }
                detected.add("Credit Card ($cardIssuer, Luhn-valid)")
                highestRisk = maxOf(highestRisk, 90)
                break
            }
        }

        // 6. Mnemonic seed phrases
        if (detectMnemonicSeedPhrase(clipboardText)) {
            detected.add("Cryptocurrency Mnemonic Seed Phrase (12-24 words)")
            highestRisk = maxOf(highestRisk, 100)
        }

        // 7. JWT or High Entropy Tokens
        if (JWT_REGEX.containsMatchIn(clipboardText)) {
            detected.add("JSON Web Token (JWT)")
            highestRisk = maxOf(highestRisk, 85)
        } else {
            // Check tokens in the text for high entropy
            val tokens = clipboardText.trim().split(Regex("""[\s,;'"]+"""))
            for (token in tokens) {
                if (token.length >= 32 && !token.contains(" ")) {
                    val ent = calculateShannonEntropy(token)
                    if (ent >= 4.2f) {
                        detected.add("High Entropy Secret Token / Secret Key (H: ${String.format(Locale.US, "%.2f", ent)})")
                        highestRisk = maxOf(highestRisk, 80)
                        break
                    }
                }
            }
        }

        val isSensitive = detected.isNotEmpty()
        val recommendation = if (isSensitive) {
            "Sensitive credential exposed in clipboard! Background apps may capture this. Clear clipboard immediately."
        } else {
            "No sensitive credentials detected in clipboard."
        }

        val clearTimer = if (highestRisk >= 90) 15 else if (highestRisk > 50) 30 else 0

        // Strict Privacy Rule: We DO NOT return or log the actual clipboardText
        return LocalClipboardResult(
            isSensitive = isSensitive,
            detectedTypes = detected,
            riskScore = highestRisk,
            recommendation = recommendation,
            suggestedClearTimerSec = clearTimer,
            valueStored = false
        )
    }
}
