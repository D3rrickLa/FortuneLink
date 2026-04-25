"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState } from "react";

// basically, react query needs a central 'brain' for keeping track of data
// we have many components, they all need a way to share the same 'cache'. without this provider
// our components wouldn't know where to look for the data being fetched, and they wouldn't be able to talk to each other
// in simple terms, if we go from page a to page b, page b wouldn't know what page a already fetched
export function Providers({ children }: { children: React.ReactNode }) {
  // we use 'useState' becasue the alternative of new QueryClient() is a new cache
  // everytime the Providers components re-renders, widping our data and doing refetching
  // useSTate ensures the client is created only once per session
  const [queryClient] = useState(
    () => new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 60 * 1000,
          retry: 1
        },
      },
    })
  )


  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}