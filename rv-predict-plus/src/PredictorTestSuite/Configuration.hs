module PredictorTestSuite.Configuration
  ( srcRoot
  , cpSuffix
  , tests
  ) where
  import qualified System.IO.Unsafe as Unsafe
  import Data.Yaml.Syck
  import Data.Char

  import System.Posix.Directory

  -- "Unsafe" and icky, but a quick way to export pretty and useful attributes

  srcRoot :: String
  srcRoot = Unsafe.unsafePerformIO getWorkingDirectory ++ "/../"

  rootMap :: [(YamlNode, YamlNode)]
  rootMap = case n_elem (Unsafe.unsafePerformIO (parseYamlFile "PredictorTestSuite/configure.yml")) of
               EMap assocL -> assocL
               _           -> error "Malformed configure.yml. The file should be a map"

  getVal :: String -> YamlElem
  getVal k = case lookupY k rootMap of
               Just val -> n_elem val
               _        -> error $ "Unable to find " ++ k ++ " in configure.yml"

  lookupY :: String -> [(YamlNode, YamlNode)] -> Maybe YamlNode
  lookupY _ [] = Nothing
  lookupY field ((k,v):ys) = case n_elem k of
                               EStr buf -> if unpackBuf buf == field then Just v else lookupY field ys
                               _        -> error "Expecting simple strings for keys"

  getTest :: String -> (Int, String, String, String, [String])
  getTest tname = case tname `lookupY` testsE of
                    Just mapN -> case n_elem mapN of
                                   EMap m -> ( extractNumber (getStr "effort" m), tname, getStr "classname" m
                                             , getStr "arguments" m, getSeq "races" m)
                                   _      -> error "Malformed configure.yml. Expecting tests to be maps"
                    _         -> error $ "Unable to find test " ++ tname ++ " in configure.yml"
    where getStr k m = case get k m of
                         EStr buf -> unpackBuf buf
                         _        -> error $ "Expecting string for " ++ k
          getSeq k m = case get k m of
                         ESeq yns -> extractStrs $ map n_elem yns
                         _        -> error $ "Expecting a sequence for " ++ k
          get k m = case k `lookupY` m of
                      Just e -> n_elem e
                      _      -> error $ "Unable to find " ++ k
          extractStrs [] = []
          extractStrs (x:xs) = case x of
                                 EStr buf -> unpackBuf buf : extractStrs xs
                                 _        -> error "Expecting a string inside this sequence"
          extractNumber s | and (map isDigit s) = read s
                          | otherwise           = error "Please give a number for effort"


  -- srcRoot :: String
  -- srcRoot = case getVal "srcRoot" of
  --             EStr buf -> unpackBuf buf
  --             _        -> error "Malformed configure.yml at srcRoot. Expecting a string"

  cpSuffix :: [String]
  cpSuffix = case getVal "cpSuffix" of
               ESeq yns -> map (getStr.n_elem) yns
               _        -> error "Malformed configure.yml at cpSuffix. Expecting a sequence"
    where getStr (EStr buf) = unpackBuf buf
          getStr _ = error "Malformed configure.yml inside cpSuffix. Expecting strings in the sequence"

  -- pun only semi-intended
  testsE :: [(YamlNode, YamlNode)]
  testsE = case getVal "Tests" of
             EMap assocL -> assocL
             _           -> error "Malformed configure.yml. Expecting Tests to be a map"

  -- The test 5-tuple
  tests :: [(Int, String, String, String, [String])]
  tests = map getTest $ keyStrs testsE
    where keyStrs ((k,_):ys) = case n_elem k of
                                 EStr buf -> unpackBuf buf : keyStrs ys
                                 _        -> error "Expecting string keys"
          keyStrs [] = []