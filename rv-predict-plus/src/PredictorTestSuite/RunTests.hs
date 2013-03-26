 -- Run the test suite

{-# OPTIONS -fglasgow-exts #-}
module PredictorTestSuite.RunTests ( main ) where

  import Data.List
  import Data.Char
--  import qualified Data.ByteString as BS
  import Data.Time.Clock
--  import Data.Yaml.Syck
--  import ProgressBar

  import System.Console.GetOpt
  import System.IO
--  import System.IO.Capture
--  import System.Directory
--  import System.TimeIt
--  import System.CPUTime
  import System.Exit
  import System.Process
  import Control.Monad.Error
  import Control.Monad.State
--  import HSH.Command

  import qualified System.Environment as Env
  import qualified PredictorTestSuite.Util as Util
  import qualified PredictorTestSuite.Configuration as Config
  import PredictorTestSuite.RaceCompare
--  import qualified System.IO.Unsafe as Unsafe

  -- todo: Have a monad for propagating (stdout,stderr), erroring with stderr.
  --       incorporate into said monad (or transform across a new one) verbosity checks at every step of the way
  --       also, timing monad



  -- instance MonadIO Verbose where
  --   liftIO io = ShowVerbose (

  -- An error
  type ExecutableErr = ErrorT String IO

  -- The program executable.
  -- todo: have executable keep around info pertaining to counting success/failure and/or timing
  type Executable a = StateT Options ExecutableErr a


  type Effort = Int

--  type TestName = String
  type PackageName = String
  type ClassName = String
  type StdErr = String
  type StdOut = String
  type CommandName = String
  type Argument = String
  type FieldName = String


  {- Command Line Arguments -}

  options :: [OptDescr (Options -> Options)]
  options =
    [ Option ['v'] ["verbose"]     (NoArg setVerbose)            "Show more output"
    , Option ['h'] ["help"]        (NoArg setHelp)               "Show help"
    , Option ['e'] ["effort"]      (ReqArg setEffort "<number>") "Specify an effort"
    , Option ['i'] ["instrument"]  (NoArg setInstrument)         "(Re)instrument before running"
    , Option ['t'] ["timing"]      (NoArg setTiming)             "Output timing info"
    , Option ['f'] ["fastfail"]    (NoArg setFastfail)           "Exit on the first failure"
    , Option ['b'] ["build"]       (NoArg setBuild)              "(re) build rv-predict"
    , Option ['p'] ["passthrough"] (NoArg setPassthrough)        "N/A Use the passthrough slicer (devel)"
    , Option ['u'] ["unfiltered"]  (NoArg setUnfiltered)         "N/A Don't filter excess stderr information"
    , Option ['s'] ["all-shared"]  (NoArg setAllShared)          "N/A Predict on all shared variables"
    , Option ['d'] ["directory"]   (ReqArg setDirectory "<dir>") $
               "Explicitly specify a directory to use. "
            ++ "Warning: If instrumenting, the directory's contents will first be removed entirely"
    ]

  data Options = Options { verbose :: Bool, help :: Bool, effort :: String --, test :: TestName
                         , instrument :: Bool, passthrough :: Bool, unfiltered :: Bool
                         , allShared :: Bool, directory :: String, readEffort :: Int
                         , timing :: Bool, fastfail :: Bool, build :: Bool }
    deriving Show

  defaults :: Options
  defaults = Options { verbose = False, help = False, effort = "0" --, test = ""
                     , instrument = False, passthrough = False, unfiltered = False
                     , allShared = False, directory = defaultOutDir, readEffort = 0
                     , timing = False, fastfail = False, build = False }


  {- Setters and Handlers for the options -}

  setHelp, setInstrument, setVerbose, setBuild, setPassthrough, setUnfiltered, setAllShared, setFastfail, setTiming :: Options -> Options
  setEffort, setDirectory :: String -> Options -> Options

  setHelp opt        = opt { help = True }
  setInstrument opt  = opt { instrument = True }
  setTiming opt      = opt { timing = True }
  setBuild opt       = opt { build = True }
  setDirectory d opt = opt { directory = d }
  setFastfail    opt = opt { fastfail = True }

  --- The rest of the options haven't been fully implemented yet. todo: finish them
  setEffort n opt    = opt { effort = n }
  setVerbose opt     = opt { verbose = True }
  setPassthrough opt = opt { passthrough = True }
  setUnfiltered opt  = opt { unfiltered = True }
  setAllShared opt   = opt { allShared = True }

  handleHelp, handleInstrument, handleDirectory :: Executable ()
  handleHelp = get >>= \opts -> when (help opts) $ throwError (usageInfo "Options:" options)

  handleVerbose :: (String,String) -> Executable ()
  handleVerbose (left,right) =
    get >>= \opts -> when (verbose opts) . outputIn . unlines . map (\s -> "\t" ++ s) . lines $ left ++ right

  handleTiming :: UTCTime -> UTCTime -> Executable ()
  handleTiming a b = get >>= \opts -> when (timing opts) $
                     do output ""
                        outputProgress  ("Elapsed time: " ++ show (diffUTCTime b a)) (40 - 7)
                        io $ hFlush stdout

  handleInstrument = get >>= \opts -> when (instrument opts) $ do
                       { execute_ "rm" ["-rf", directory opts]
                       ; execute_ "mkdir" [directory opts]
                       ; output "Instrumenting the following packages: "
                       ; output $ "  " ++ (concat . intersperse " ") packages
                       ; start <- time
                       ; (out, err) <- execute "java" $ instr opts
                       --                     ; handleVerbose (out,err)
                       ; end <- time
                       ; outputIn "Done"
                       ; handleTiming start end
                       ; output ""
                       ; return ()
                       } `catchError` (\ err -> output err)
    where instr opts = [ "-ea", "-cp", instCp, "jpredictor.instrumentation.Main", "-app", "account.Main", "-d"
                       , directory opts, "-validate", "-x", "com.google.protobuf"] ++ packs
          packs = "-dynamic-package" : intersperse "-dynamic-package" packages

  handleDirectory = get >>= \opts -> output $ "Using directory: " ++ directory opts
  handleFastfail = get >>= \opts -> when (fastfail opts) $ throwError "Exiting on failure"

  handleBuild = get >>= \opts -> when (build opts) $ do
                  start <- time
                  outputIn "Building rv-predict..."
                  (out,err) <- execute (utilScriptDir ++ "javacExamples") []
                  outputIn "Done"
                  end <- time
                  handleTiming start end
                  output ""

  handleEffort :: Executable ()
  handleEffort = do { opts <- get
                    ; unless (and (map isDigit (effort opts))) $
                             throwError $ ("Unable to read '" ++ (effort opts) ++ "', give a number for effort")
                    ; put opts { readEffort = (read (effort opts)) }
                    }

  -- isDifferent :: Eq a => (Options -> a) -> Options -> Bool
  -- isDifferent acc opts = acc opts == acc defaults

  {- Actions to perform -}

  -- If there are errors, then throw them, else continue
  checkErrs :: [String] -> Executable ()
  checkErrs l = unless (null l) $ throwError (concat l)

  runTests :: Executable ()
  runTests = do { opts <- get
                ; output $ "Using effort: " ++ show (readEffort opts)
                ; start <- time
                ; mapM_ (Util.uncurry5 runTest) (filter (\(e,_,_,_,_) -> e <= readEffort opts) tests)
                ; end <- time
                ; output ""
                ; outputIn "Done"
                ; handleTiming start end
                ; output ""
                } `catchError` (\ err -> output err)

  runTest :: Int -> PackageName -> ClassName -> String -> [String] -> Executable ()
  runTest _ pac c args expectedRaces = do
    opts <- get
    start <- time
    output ""
    output ("Testing: " ++ pac ++ "        ")
    outputProgress "Running" 40
    (out,err) <- execute runTestLoc $ [directory opts, directory opts, pac, c] ++ words args
    outputProgress "Predicting" 7
    (out,err) <- execute "java" (predict opts)
    outputProgress "Diffing" 10
    diffRaces out pac
    end <- time
    handleTiming start end
    where predict opts = [ "-ea", "-cp", directory opts ++ "../:" ++ baseCp, "jpredictor.Main", "-app"
                         , pac ++ "." ++ c, "-d", directory opts, "-validate" ]
          diffRaces out _ = compRaces out (expectedRaces)
          compRaces out ref = case compareRacesWithExpected out ref of
                                ([],[]) -> outputProgress "[Ok]           " (7)
                                (x,y) -> outputProgress " [FAIL] " 0 >> outputDifferences x y >> handleFastfail
          outputDifferences [] [] = return ()
          outputDifferences [] unfound = do output "  Failed to find races on the following fields:"
                                            mapM_ (\f -> output ("    " ++ f)) unfound
          outputDifferences unspecified [] = do output "  Found extra unspecified races on the following fields:"
                                                mapM_ (\f -> output ("    " ++ f)) unspecified
          outputDifferences unspecified unfound = outputDifferences unspecified [] >> outputDifferences [] unfound


  -- output information progress-bar style (that is, clean up the current line before printing).
  outputProgress :: String -> Int -> Executable ()
  outputProgress s i = io $ Util.backspace i >> putStr s >> hFlush stdout

  -- Suppress uneeded info, such as from the cachemanager. Currently there's nothing superfluous anyways
  -- todo: implement.
  -- suppressNeedless :: String -> String
  -- suppressNeedless = id

  -- Main entry point
  main :: IO ()
  main = do args <- liftIO Env.getArgs
            let (actions, nonOpts, msgs) = getOpt Permute options args
            _ <- runErrorT $ runStateT (realMain actions nonOpts msgs) defaults
            return ()

  -- Where the real work happens
  -- todo: figure out what behavior I want for the nonOpts arguments
  realMain :: [Options -> Options] -> [String] -> [String] -> Executable ()
  realMain actions _ errs = do { checkErrs errs
                               ; javacExamples
                               ; put processArgs
                               ; handleHelp
                               ; handleEffort
                               ; handleDirectory
                               ; handleBuild
                               ; handleInstrument
                               ; runTests
                               ; return ()
                               } `catchError` (\ err -> output err)
                                 where processArgs = foldl (\ a b -> b a) defaults actions
    -- todo: do more for the other options


  {- Paths and directories -}

  srcRoot, libDir, testScriptDir, exampleDir, baseCp, instCp, defaultOutDir, runTestLoc :: String

  mkPath :: String -> String
  mkPath s = srcRoot ++ s

  srcRoot = Config.srcRoot
  exampleDir = mkPath "examples/prediction/"
  testScriptDir = mkPath "bin/tests/"
  utilScriptDir = mkPath "bin/util/"
  libDir = mkPath "lib/"

  runTestLoc = testScriptDir ++ "runTest"
  defaultOutDir = exampleDir ++ ".instrumented_tests/"

  baseCp = concat . intersperse ":" . map (libDir ++) $ Config.cpSuffix
  instCp = baseCp ++ ":" ++ exampleDir

  tests ::  [(Effort, PackageName, ClassName, String, [FieldName])]
  tests = Config.tests

  packages :: [PackageName]
  packages = map snd tests
    where snd (_,x,_,_,_) = x



  -- javac all the files
  -- in the future maybe have something a little more intelligent
  javacExamples :: Executable ()
  javacExamples = execute_ (utilScriptDir ++ "javacExamples") []

  {- Misc utility -}

  -- Perform the IO action, return the result as an Executable.
  -- Wrapper that can be useful in some cases to disambiguate types where it's used
  io :: IO a -> Executable a
  io = liftIO

  -- Output the string, wrapped up into an Executable
  output :: String -> Executable ()
  output = io.putStrLn

  -- As output, but no newline
  outputIn :: String -> Executable ()
  outputIn = io.putStr

  -- Execute a shell command, return a (stdout, stderr) on success. Throws stderr on error
  -- Uses handleVerbose to output stuff if in verbose mode
  execute :: CommandName -> [Argument] -> Executable (StdOut, StdErr)
  execute cmd args = do (ex,out,err) <- io $ readProcessWithExitCode cmd args  ""
                        case ex of ExitSuccess -> handleVerbose (out,err) >> return (out,err)
                                   _           -> throwError (err)

  -- Execute, discard everything. Don't throw anything.
  execute_ :: CommandName -> [Argument] -> Executable ()
  execute_ cmd args = (execute cmd args >> return ()) `catchError` (\_ -> return ())

  time :: Executable UTCTime
  time = io getCurrentTime


