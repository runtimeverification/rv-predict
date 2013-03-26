-- Compare two races for equivalency

-- Currently constructs elaborate ADTs, then discards them and drives into walls. I mean, compares fieldnames

{-# OPTIONS -fglasgow-exts #-}
module PredictorTestSuite.RaceCompare
  ( compareRaces
  , compareRacesWithExpected
  )
  where

--  import Data.List
  import Text.Regex
  import PredictorTestSuite.Util

  -- As compare races, but we already have some expect fields to compare to
  compareRacesWithExpected :: String -> [String] -> ([String],[String])
  compareRacesWithExpected rs expected = (remainingLeft, remainingRight)
    where toFields = map getField . splitRaces
          (left,right) = (toFields rs, expected)
          (remainingLeft, remainingRight) = (right `deletes` left, left `deletes` right)

  -- Compare strings of races, returning a pair of the races that do not exist in the other
  -- e.g. compareRaces "r1 r2 r3" "r2 r4 r5" === (["r1", "r3"], ["r4", "r5"])
  -- Identical races are still considered distinct, this compareRaces "r1 r1" "r1" === (["r1"], [])
  --   where r1, r2, etc are actual race reports
  compareRaces :: String -> String -> ([String],[String])
  compareRaces r1 r2 = (remainingLeft, remainingRight)
    where toFields = map getField . splitRaces
          (left,right) = (toFields r1, toFields r2)
          (remainingLeft, remainingRight) = (right `deletes` left, left `deletes` right)


  -- Seperate multiple races out of a single string into a list of the race headers
  splitRaces :: String -> [String]
  splitRaces = filter isRaceHeader . lines
    where isRaceHeader s = doesMatch (mkRegex ".* Race found .*") s

  -- Compare two races, to see if they have the same field
  -- sameFields :: String -> String -> Bool
  -- sameFields r1 r2 = getField (head (lines r1)) == getField (head (lines r2))

  -- Get the fieldname from a race header, drop off the instance number and such
  getField :: String -> String
  getField = head.words.fieldStr
    where fieldStr s = case mkRegex  ".* Race found on (.*) ---.*" `matchRegex` s of
                         Just [field] -> field
                         _ -> error "Unable to parse race header line: " ++ s

  -- Whether a regex matches a string
  doesMatch :: Regex -> String -> Bool
  doesMatch r s = case r `matchRegex` s of
                    Nothing -> False
                    Just _ -> True




  {- Rest of file is defs and examples for testing this file itself -}


  -- rC1, rC2, rCs :: String

  -- rC1 = "\n /--- Race found on simple.Simple.i ---\\ \n |  Read  at simple.Simple:10          | \n |  Write at simple.Simple:10          | \n \\-------------------------------------/\n"

  -- rC2 = "\n /--- Race found on java.io.PrintStream (instance #447267976) ---\\ \n |  Write at simple.Simple:11                                    | \n |  Write at simple.Simple:11                                    | \n \\---------------------------------------------------------------/\n"


  -- rCs = "\n" ++ rC1 ++ rC2

