import { Helmet } from 'react-helmet';

function Head({ title, description, keywords }) {
  return (
    <Helmet>
      {/* General */}
      <html lang="en" />
      <title>{title}</title>
      <meta name="description" content={description} />
      <meta name="keywords" content={keywords} />

      {/* Twitter card */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:image" content="https://www.flybot.sg/static/media/flybot-logo.5c92e3d50082cfa03409.png" />
      <meta
        name="twitter:title"
        content={title}
      />
      <meta
        name="twitter:description"
        content={description}
      />

      {/* Facebook */}
      <meta property="og:type" content="website" />
      <meta property="og:url" content="www.flybot.sg" />
      <meta
        property="og:title"
        content={title}
      />
      <meta
        property="og:description"
        content={description}
      />
      <meta
        property="og:image"
        content="https://www.flybot.sg/static/media/flybot-logo.5c92e3d50082cfa03409.png"
      />
    </Helmet>
  )
}

Head.defaultProps = {
  title: "Flybot",
  description: "Software company leveraging the functional programming language Clojure. We design and implement systems to solve complex problems in a simple and scalable way.",
  keywords: "Clojure, Functional Programming, Development, Singapore, Code"
};

export default Head